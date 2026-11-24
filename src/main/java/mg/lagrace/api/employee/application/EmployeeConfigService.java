package mg.lagrace.api.employee.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mg.lagrace.api.auth.User;
import mg.lagrace.api.employee.dto.DocumentRequest;
import mg.lagrace.api.employee.dto.EmployeeRequest;
import mg.lagrace.api.employee.dto.EmployeeResponse;
import mg.lagrace.api.employee.dto.DocumentResponse;
import mg.lagrace.api.employee.dto.JobRequest;
import mg.lagrace.api.employee.dto.JobResponse;
import mg.lagrace.api.employee.dto.NameRequest;
import mg.lagrace.api.employee.ContractType;
import mg.lagrace.api.employee.Department;
import mg.lagrace.api.employee.Document;
import mg.lagrace.api.employee.DocumentType;
import mg.lagrace.api.employee.Employee;
import mg.lagrace.api.employee.Job;
import mg.lagrace.api.employee.LeaveType;
import mg.lagrace.api.exceptions.DuplicateResourceException;
import mg.lagrace.api.exceptions.ResourceNotFoundException;
import mg.lagrace.api.exceptions.ValidationException;
import mg.lagrace.api.reference.Nationality;
import mg.lagrace.api.reference.Sex;
import mg.lagrace.api.employee.persistence.ContractTypeRepository;
import mg.lagrace.api.employee.persistence.DepartmentRepository;
import mg.lagrace.api.employee.persistence.DocumentRepository;
import mg.lagrace.api.employee.persistence.DocumentTypeRepository;
import mg.lagrace.api.employee.persistence.EmployeeRepository;
import mg.lagrace.api.employee.persistence.JobRepository;
import mg.lagrace.api.employee.persistence.LeaveTypeRepository;
import mg.lagrace.api.reference.persistence.NationalityRepository;
import mg.lagrace.api.reference.persistence.SexRepository;
import mg.lagrace.api.auth.persistence.UserRepository;

@Service
public class EmployeeConfigService {
	private final EmployeeRepository employeeRepository;
	private final DocumentRepository documentRepository;
	private final SexRepository sexRepository;
	private final NationalityRepository nationalityRepository;
	private final JobRepository jobRepository;
	private final ContractTypeRepository contractTypeRepository;
	private final DepartmentRepository departmentRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final LeaveTypeRepository leaveTypeRepository;
	private final UserRepository userRepository;

	public EmployeeConfigService(
			EmployeeRepository employeeRepository,
			DocumentRepository documentRepository,
			SexRepository sexRepository,
			NationalityRepository nationalityRepository,
			JobRepository jobRepository,
			ContractTypeRepository contractTypeRepository,
			DepartmentRepository departmentRepository,
			DocumentTypeRepository documentTypeRepository,
			LeaveTypeRepository leaveTypeRepository,
			UserRepository userRepository) {
		this.employeeRepository = employeeRepository;
		this.documentRepository = documentRepository;
		this.sexRepository = sexRepository;
		this.nationalityRepository = nationalityRepository;
		this.jobRepository = jobRepository;
		this.contractTypeRepository = contractTypeRepository;
		this.departmentRepository = departmentRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.leaveTypeRepository = leaveTypeRepository;
		this.userRepository = userRepository;
	}

    // ======== DEPARTMENT =========
	public List<Department> getDepartments() {
		return departmentRepository.findAll();
	}

	public Department addDepartment(NameRequest request) {
		String name = validateAndNormalizeName(request, "du département");
		if (departmentRepository.existsByName(name)) {
			throw new DuplicateResourceException("Le département '" + name + "' existe déjà.");
		}
		Department department = new Department();
		department.setName(name);
		return departmentRepository.save(department);
	}

	public Department updateDepartment(Long id, NameRequest request) {
		Department department = findDepartment(id);
		String name = validateAndNormalizeName(request, "du département");
		if (!department.getName().equals(name) && departmentRepository.existsByName(name)) {
			throw new DuplicateResourceException("Le département '" + name + "' existe déjà.");
		}
		department.setName(name);
		return departmentRepository.save(department);
	}

	public void deleteDepartment(Long id) {
		departmentRepository.delete(findDepartment(id));
	}

    // ========= JOB =========
	@Transactional(readOnly = true)
	public List<JobResponse> getJobs() {
		return jobRepository.findAll().stream().map(this::toJobResponse).toList();
	}

	public JobResponse addJob(JobRequest request) {
		validateJobRequest(request);
		Department department = findDepartment(request.departmentId());
		String name = request.name().trim();
		if (jobRepository.existsByDepartmentIdDepartmentAndName(request.departmentId(), name)) {
			throw new DuplicateResourceException("Ce poste existe déjà dans ce département.");
		}
		Job job = new Job();
		job.setName(name);
		job.setDepartment(department);
		return toJobResponse(jobRepository.save(job));
	}

	public JobResponse updateJob(Long id, JobRequest request) {
		validateId(id, "le poste");
		validateJobRequest(request);
		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Poste introuvable avec l'id " + id));
		Department department = findDepartment(request.departmentId());
		String name = request.name().trim();
		if ((!job.getName().equals(name) || !job.getDepartment().getIdDepartment().equals(request.departmentId()))
				&& jobRepository.existsByDepartmentIdDepartmentAndNameAndIdJobNot(request.departmentId(), name, id)) {
			throw new DuplicateResourceException("Ce poste existe déjà dans ce département.");
		}
		job.setName(name);
		job.setDepartment(department);
		return toJobResponse(jobRepository.save(job));
	}

	private JobResponse toJobResponse(Job job) {
		return new JobResponse(job.getIdJob(), job.getName(), job.getDepartment().getIdDepartment(), job.getDepartment().getName());
	}

	public void deleteJob(Long id) {
		validateId(id, "le poste");
		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Poste introuvable avec l'id " + id));
		jobRepository.delete(job);
	}

    // ========= TYPE CONTRAT =========
	public List<ContractType> getContractTypes() {
		return contractTypeRepository.findAll();
	}

	public ContractType addContractType(NameRequest request) {
		return saveContractType(request, null);
	}

	public ContractType updateContractType(Long id, NameRequest request) {
		ContractType contractType = contractTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Type de contrat introuvable avec l'id " + id));
		return saveContractType(request, contractType);
	}

	public void deleteContractType(Long id) {
		validateId(id, "le type de contrat");
		if (!contractTypeRepository.existsById(id)) {
			throw new ResourceNotFoundException("Type de contrat introuvable avec l'id " + id);
		}
		contractTypeRepository.deleteById(id);
	}

    // ========= TYPE DOCUMENT =========
	public List<DocumentType> getDocumentTypes() {
		return documentTypeRepository.findAll();
	}

	public DocumentType addDocumentType(NameRequest request) {
		return saveDocumentType(request, null);
	}

	public DocumentType updateDocumentType(Long id, NameRequest request) {
		DocumentType documentType = documentTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Type de document introuvable avec l'id " + id));
		return saveDocumentType(request, documentType);
	}

	public void deleteDocumentType(Long id) {
		validateId(id, "le type de document");
		if (!documentTypeRepository.existsById(id)) {
			throw new ResourceNotFoundException("Type de document introuvable avec l'id " + id);
		}
		documentTypeRepository.deleteById(id);
	}

    // ========= TYPE CONGES =========
	public List<LeaveType> getLeaveTypes() {
		return leaveTypeRepository.findAll();
	}

	public LeaveType addLeaveType(NameRequest request) {
		return saveLeaveType(request, null);
	}

	public LeaveType updateLeaveType(Long id, NameRequest request) {
		LeaveType leaveType = leaveTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Type de congé introuvable avec l'id " + id));
		return saveLeaveType(request, leaveType);
	}

	public void deleteLeaveType(Long id) {
		validateId(id, "le type de congé");
		if (!leaveTypeRepository.existsById(id)) {
			throw new ResourceNotFoundException("Type de congé introuvable avec l'id " + id);
		}
		leaveTypeRepository.deleteById(id);
	}

    // ========= UTILS ===========
	private Department findDepartment(Long id) {
		validateId(id, "le département");
		return departmentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Département introuvable avec l'id " + id));
	}

	private String validateAndNormalizeName(NameRequest request, String resource) {
		if (request == null || request.name() == null || request.name().isBlank()) {
			throw new ValidationException("Le nom " + resource + " est obligatoire.");
		}
		return request.name().trim();
	}

	private void validateJobRequest(JobRequest request) {
		if (request == null || request.name() == null || request.name().isBlank()) {
			throw new ValidationException("Le nom du poste est obligatoire.");
		}
		validateId(request.departmentId(), "le département");
	}

	private ContractType saveContractType(NameRequest request, ContractType contractType) {
		String name = validateAndNormalizeName(request, "du type de contrat");
		if ((contractType == null || !contractType.getName().equals(name))
				&& contractTypeRepository.existsByName(name)) {
			throw new DuplicateResourceException("Le type de contrat '" + name + "' existe déjà.");
		}
		if (contractType == null) {
			contractType = new ContractType();
		}
		contractType.setName(name);
		return contractTypeRepository.save(contractType);
	}

	private DocumentType saveDocumentType(NameRequest request, DocumentType documentType) {
		String name = validateAndNormalizeName(request, "du type de document");
		if ((documentType == null || !documentType.getName().equals(name))
				&& documentTypeRepository.existsByName(name)) {
			throw new DuplicateResourceException("Le type de document '" + name + "' existe déjà.");
		}
		if (documentType == null) {
			documentType = new DocumentType();
		}
		documentType.setName(name);
		return documentTypeRepository.save(documentType);
	}

	private LeaveType saveLeaveType(NameRequest request, LeaveType leaveType) {
		String name = validateAndNormalizeName(request, "du type de congé");
		if ((leaveType == null || !leaveType.getName().equals(name))
				&& leaveTypeRepository.existsByName(name)) {
			throw new DuplicateResourceException("Le type de congé '" + name + "' existe déjà.");
		}
		if (leaveType == null) {
			leaveType = new LeaveType();
		}
		leaveType.setName(name);
		return leaveTypeRepository.save(leaveType);
	}


    // =============== EMPLOYEE ==============
	@Transactional(readOnly = true)
	public List<EmployeeResponse> getEmployees() {
		return employeeRepository.findAll().stream().map(this::toEmployeeResponse).toList();
	}

	public Employee getEmployeeEntity(Long id) {
		validateId(id, "l'employé");
		return employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employé introuvable avec l'id " + id));
	}

	@Transactional(readOnly = true)
	public EmployeeResponse getEmployee(Long id) {
		return toEmployeeResponse(getEmployeeEntity(id));
	}

	public Employee addEmployee(EmployeeRequest request) {
		validateEmployeeRequest(request);
		Employee employee = new Employee();
		applyEmployeeRequest(employee, request, null);
		return employeeRepository.save(employee);
	}

	public Employee updateEmployee(Long id, EmployeeRequest request) {
		validateId(id, "l'employé");
		validateEmployeeRequest(request);
		Employee employee = getEmployeeEntity(id);
		applyEmployeeRequest(employee, request, id);
		return employeeRepository.save(employee);
	}

	public void deleteEmployee(Long id) {
		Employee employee = getEmployeeEntity(id);
		employeeRepository.delete(employee);
	}

    // =============== DOCUMENTS ==============
	@Transactional(readOnly = true)
	public List<DocumentResponse> getDocuments() {
		return documentRepository.findAll().stream().map(this::toDocumentResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> getDocumentsByEmployee(Long employeeId) {
		getEmployeeEntity(employeeId);
		return documentRepository.findByEmployeeIdEmployee(employeeId).stream().map(this::toDocumentResponse).toList();
	}

	public Document addDocument(DocumentRequest request) {
		validateDocumentRequest(request);
		Document document = new Document();
		applyDocumentRequest(document, request);
		return documentRepository.save(document);
	}

	public Document updateDocument(Long id, DocumentRequest request) {
		validateId(id, "le document");
		validateDocumentRequest(request);
		Document document = documentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Document introuvable avec l'id " + id));
		applyDocumentRequest(document, request);
		return documentRepository.save(document);
	}

	public void deleteDocument(Long id) {
		validateId(id, "le document");
		if (!documentRepository.existsById(id)) {
			throw new ResourceNotFoundException("Document introuvable avec l'id " + id);
		}
		documentRepository.deleteById(id);
	}

    // =============== EMPLOYEE UTILS ==============
	private void applyEmployeeRequest(Employee employee, EmployeeRequest request, Long currentEmployeeId) {
		employee.setLastName(request.lastName().trim());
		employee.setFirstName(request.firstName().trim());
		employee.setDateOfBirth(request.dateOfBirth());
		employee.setPhone(normalizeOptional(request.phone()));
		employee.setAddress(normalizeOptional(request.address()));
		employee.setSex(findSex(request.sexId()));
		employee.setNationality(request.nationalityId() == null ? null : findNationality(request.nationalityId()));
		employee.setJob(findJob(request.jobId()));
		employee.setContractType(findContractType(request.contractTypeId()));
		employee.setHireDate(request.hireDate());
		employee.setEndDate(request.endDate());
		employee.setBaseSalary(request.baseSalary());

		if (request.userId() == null) {
			employee.setUser(null);
		} else {
			employeeRepository.findByUserIdUser(request.userId())
					.filter(existing -> !existing.getIdEmployee().equals(currentEmployeeId))
					.ifPresent(existing -> {
						throw new DuplicateResourceException("Cet utilisateur est déjà associé à un employé.");
					});
			employee.setUser(findUser(request.userId()));
		}
	}

	private void applyDocumentRequest(Document document, DocumentRequest request) {
		document.setEmployee(getEmployeeEntity(request.employeeId()));
		document.setType(documentTypeRepository.findById(request.typeId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Type de document introuvable avec l'id " + request.typeId())));
		document.setDocumentNumber(normalizeOptional(request.documentNumber()));
		document.setFilePath(normalizeOptional(request.filePath()));
	}

	private void validateEmployeeRequest(EmployeeRequest request) {
		if (request == null) {
			throw new ValidationException("Les données de l'employé sont obligatoires.");
		}
		validateText(request.lastName(), "Le nom de famille");
		validateText(request.firstName(), "Le prénom");
		validateRequiredId(request.sexId(), "du sexe");
		validateRequiredId(request.jobId(), "du poste");
		validateRequiredId(request.contractTypeId(), "du type de contrat");
		if (request.hireDate() == null) {
			throw new ValidationException("La date d'embauche est obligatoire.");
		}
		if (request.endDate() != null && request.endDate().isBefore(request.hireDate())) {
			throw new ValidationException("La date de fin doit être postérieure ou égale à la date d'embauche.");
		}
		if (request.baseSalary() != null && request.baseSalary().signum() < 0) {
			throw new ValidationException("Le salaire de base ne peut pas être négatif.");
		}
	}

	private void validateDocumentRequest(DocumentRequest request) {
		if (request == null) {
			throw new ValidationException("Les données du document sont obligatoires.");
		}
		validateRequiredId(request.employeeId(), "de l'employé");
		validateRequiredId(request.typeId(), "du type de document");
	}

	private void validateText(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new ValidationException(label + " est obligatoire.");
		}
	}

	private void validateRequiredId(Long id, String resource) {
		if (id == null) {
			throw new ValidationException("L'identifiant " + resource + " est obligatoire.");
		}
	}

	private void validateId(Long id, String resource) {
		if (id == null) {
			throw new ValidationException("L'identifiant de " + resource + " est obligatoire.");
		}
	}

	private Sex findSex(Long id) {
		return sexRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sexe introuvable avec l'id " + id));
	}

	private Nationality findNationality(Long id) {
		return nationalityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Nationalité introuvable avec l'id " + id));
	}

	private Job findJob(Long id) {
		return jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Poste introuvable avec l'id " + id));
	}

	private ContractType findContractType(Long id) {
		return contractTypeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Type de contrat introuvable avec l'id " + id));
	}

	private User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'id " + id));
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private EmployeeResponse toEmployeeResponse(Employee employee) {
		var job = employee.getJob();
		var department = job.getDepartment();
		var sex = employee.getSex();
		var nationality = employee.getNationality();
		var contractType = employee.getContractType();
		return new EmployeeResponse(employee.getIdEmployee(), employee.getLastName(), employee.getFirstName(),
				employee.getDateOfBirth(), employee.getPhone(), employee.getAddress(), sex.getIdSex(), sex.getName(),
				nationality == null ? null : nationality.getIdNationality(), nationality == null ? null : nationality.getName(),
				job.getIdJob(), job.getName(), department.getIdDepartment(), department.getName(),
				contractType.getIdContractType(), contractType.getName(), employee.getHireDate(), employee.getEndDate(),
				employee.getBaseSalary(), employee.getUser() == null ? null : employee.getUser().getIdUser());
	}

	private DocumentResponse toDocumentResponse(Document document) {
		var employee = document.getEmployee();
		var type = document.getType();
		return new DocumentResponse(document.getIdDocument(), employee.getIdEmployee(),
				employee.getFirstName() + " " + employee.getLastName(), type.getIdType(), type.getName(),
				document.getDocumentNumber(), document.getFilePath(), document.getCreatedAt());
	}
}
