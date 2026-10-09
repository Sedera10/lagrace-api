package mg.lagrace.api.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import mg.lagrace.api.dto.photo.PhotoResponse;
import mg.lagrace.api.dto.photo.UpdatePhotoRequest;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.models.Photo;
import mg.lagrace.api.models.enums.PhotoItemType;
import mg.lagrace.api.models.resolver.ItemResolver;
import mg.lagrace.api.repositories.PhotoRepository;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final PhotoRepository photoRepo;
    private final Cloudinary cloudinary;
    private final List<ItemResolver> resolverList;

    private Map<PhotoItemType, ItemResolver> resolvers;

    @PostConstruct
    void init() {
        resolvers = resolverList.stream()
                .collect(Collectors.toMap(ItemResolver::supports, Function.identity()));
    }

    // ===================== UPLOAD =====================

    @Transactional
    public PhotoResponse upload(PhotoItemType type, Long itemId, MultipartFile file,
                                String caption, String altText, boolean isCover) throws IOException {

        validateImage(file);
        ItemResolver resolver = resolverFor(type);
        if (!resolver.exists(itemId)) {
            throw new IllegalArgumentException(type + " " + itemId + " introuvable");
        }
        String cleanCaption = normalize(caption, "La légende");
        String cleanAlt = normalize(altText, "Le texte alternatif");

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "folder", "hotel/" + type.getFolder() + "/" + itemId,
                        "public_id", UUID.randomUUID().toString(),
                        "overwrite", false,
                        "quality", "auto",
                        "fetch_format", "auto"
                )
        );

        String secureUrl = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        if (secureUrl == null || publicId == null) {
            throw new IllegalStateException("Cloudinary n'a pas retourné les informations de l'image");
        }

        try {
            if (isCover) {
                photoRepo.clearCover(type, itemId);
            }

            Photo photo = new Photo();
            photo.setItemType(type);
            photo.setItemId(itemId);
            photo.setUrl(secureUrl);
            photo.setPublicId(publicId);
            photo.setCaption(cleanCaption);
            photo.setAltText(cleanAlt);
            photo.setPosition(photoRepo.nextPosition(type, itemId));
            photo.setCover(isCover);

            Photo saved = photoRepo.saveAndFlush(photo);
            return toResponse(saved, resolver.getName(itemId));

        } catch (RuntimeException e) {
            // La base a échoué : on évite de laisser une image orpheline chez Cloudinary
            destroyQuietly(publicId);
            throw e;
        }
    }

    // ===================== LISTE =====================

    @Transactional(readOnly = true)
    public List<PhotoResponse> listByItem(PhotoItemType type, Long itemId) {
        List<Photo> photos = photoRepo.findByItemTypeAndItemIdOrderByPositionAsc(type, itemId);
        String name = resolverFor(type).getName(itemId);
        return photos.stream().map(p -> toResponse(p, name)).toList();
    }

    /** Toutes les photos (page galerie globale), avec le nom résolu en 1 requête par type. */
    @Transactional(readOnly = true)
    public List<PhotoResponse> listAll(PhotoItemType typeFilter) {
        Sort sort = Sort.by("itemType", "itemId", "position");
        List<Photo> photos = (typeFilter == null)
                ? photoRepo.findAll(sort)
                : photoRepo.findByItemType(typeFilter, sort);

        Map<PhotoItemType, Map<Long, String>> names = photos.stream()
                .collect(Collectors.groupingBy(
                        Photo::getItemType,
                        Collectors.mapping(Photo::getItemId, Collectors.toSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> resolverFor(e.getKey()).getNames(e.getValue())));

        return photos.stream()
                .map(p -> toResponse(p,
                        names.getOrDefault(p.getItemType(), Map.of())
                             .getOrDefault(p.getItemId(), "Inconnu")))
                .toList();
    }

    // ===================== MODIFIER =====================

    @Transactional
    public PhotoResponse update(Long photoId, UpdatePhotoRequest req) {
        if (req == null) {
            throw new ValidationException("Les données de modification sont obligatoires.");
        }
        if (Boolean.TRUE.equals(req.cover())) {
            setCover(photoId);              // gère le clear + recharge
        }
        Photo photo = findOrThrow(photoId); // entité propre et managée

        if (req.caption() != null)  photo.setCaption(normalize(req.caption(), "La légende"));
        if (req.altText() != null)  photo.setAltText(normalize(req.altText(), "Le texte alternatif"));
        if (req.position() != null) photo.setPosition(req.position());
        if (Boolean.FALSE.equals(req.cover())) photo.setCover(false);

        return toResponse(photo, resolverFor(photo.getItemType()).getName(photo.getItemId()));
    }

    @Transactional
    public void setCover(Long photoId) {
        Photo photo = findOrThrow(photoId);
        if (photo.isCover()) return;
        photoRepo.clearCover(photo.getItemType(), photo.getItemId());
        Photo fresh = findOrThrow(photoId);
        fresh.setCover(true);
    }

    @Transactional(readOnly = true)
    public void assertBelongsTo(Long photoId, PhotoItemType type, Long itemId) {
        Photo photo = findOrThrow(photoId);
        if (type == null || itemId == null
                || photo.getItemType() != type || !itemId.equals(photo.getItemId())) {
            throw new ValidationException("Cette photo n'appartient pas à l'élément demandé.");
        }
    }

    // ===================== SUPPRIMER =====================

    @Transactional
    public void delete(Long photoId) throws IOException {
        Photo photo = findOrThrow(photoId);
        destroyOnCloudinary(photo.getPublicId());
        photoRepo.delete(photo);
    }

    /** À appeler quand on supprime une chambre / un espace / un employé. */
    @Transactional
    public void deleteAllFor(PhotoItemType type, Long itemId) throws IOException {
        List<Photo> photos = photoRepo.findByItemTypeAndItemIdOrderByPositionAsc(type, itemId);
        for (Photo photo : photos) {
            destroyOnCloudinary(photo.getPublicId());
        }
        photoRepo.deleteAll(photos);
    }

    // ===================== OUTILS PRIVÉS =====================

    private ItemResolver resolverFor(PhotoItemType type) {
        if (type == null) {
            throw new ValidationException("Le type de photo est obligatoire.");
        }
        ItemResolver r = resolvers.get(type);
        if (r == null) {
            throw new ValidationException("Type de photo non pris en charge : " + type);
        }
        return r;
    }

    private Photo findOrThrow(Long id) {
        if (id == null) {
            throw new ValidationException("L'identifiant de la photo est obligatoire.");
        }
        return photoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo introuvable avec l'id : " + id));
    }

    private PhotoResponse toResponse(Photo p, String itemName) {
        return new PhotoResponse(p.getId(), p.getItemType(), p.getItemId(), itemName,
                p.getUrl(), p.getCaption(), p.getAltText(), p.getPosition(), p.isCover());
    }

    private void destroyOnCloudinary(String publicId) throws IOException {
        Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        String status = (String) result.get("result");
        if (!"ok".equals(status) && !"not found".equals(status)) {
            throw new IllegalStateException("La suppression Cloudinary a échoué : " + status);
        }
    }

    private void destroyQuietly(String publicId) {
        try {
            destroyOnCloudinary(publicId);
        } catch (Exception ignored) {
            // on ne masque pas l'erreur d'origine
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Veuillez sélectionner une image");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("La taille maximale autorisée est de 5 Mo");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("Formats acceptés : JPEG, PNG et WebP");
        }
    }

    private String normalize(String value, String label) {
        if (value == null || value.isBlank()) return null;
        String n = value.trim();
        if (n.length() > 255) {
            throw new ValidationException(label + " ne doit pas dépasser 255 caractères");
        }
        return n;
    }
}
