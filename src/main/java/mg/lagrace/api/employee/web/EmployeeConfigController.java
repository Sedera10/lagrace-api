package mg.lagrace.api.employee.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mg.lagrace.api.shared.api.ApiResponse;
import mg.lagrace.api.employee.dto.JobRequest;
import mg.lagrace.api.employee.dto.JobResponse;
import mg.lagrace.api.employee.dto.NameRequest;
import mg.lagrace.api.employee.ContractType;
import mg.lagrace.api.employee.Department;
import mg.lagrace.api.employee.DocumentType;
import mg.lagrace.api.employee.LeaveType;
import mg.lagrace.api.employee.application.EmployeeConfigService;
import mg.lagrace.api.employee.dto.DocumentRequest;
import mg.lagrace.api.employee.dto.DocumentResponse;
import mg.lagrace.api.employee.dto.EmployeeRequest;
import mg.lagrace.api.employee.dto.EmployeeResponse;
import mg.lagrace.api.media.PhotoItemType;
import mg.lagrace.api.media.application.PhotoService;
import mg.lagrace.api.media.dto.photo.PhotoResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


	@RestController
	@RequestMapping("/employee-configs")
	public class EmployeeConfigController {
		private final EmployeeConfigService service;
		private final PhotoService photoService;

		public EmployeeConfigController(EmployeeConfigService service, PhotoService photoService) {
			this.service = service;
			this.photoService = photoService;
		}

		@GetMapping("/departments")
		public ResponseEntity<ApiResponse<List<Department>>> getDepartments() {
			return ResponseEntity.ok(ApiResponse.success(service.getDepartments()));
		}

		@PostMapping("/departments")
		public ResponseEntity<ApiResponse<Department>> addDepartment(@RequestBody NameRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addDepartment(request), "Département créé avec succès."));
		}

		@PutMapping("/departments/{id}")
		public ResponseEntity<ApiResponse<Department>> updateDepartment(
				@PathVariable Long id, @RequestBody NameRequest request) {
			return ResponseEntity.ok(ApiResponse.success(
					service.updateDepartment(id, request), "Département mis à jour avec succès."));
		}

		@DeleteMapping("/departments/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
			service.deleteDepartment(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Département supprimé avec succès."));
		}

		@GetMapping("/jobs")
		public ResponseEntity<ApiResponse<List<JobResponse>>> getJobs() {
			return ResponseEntity.ok(ApiResponse.success(service.getJobs()));
		}

		@PostMapping("/jobs")
		public ResponseEntity<ApiResponse<JobResponse>> addJob(@RequestBody JobRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addJob(request), "Poste créé avec succès."));
		}

		@PutMapping("/jobs/{id}")
		public ResponseEntity<ApiResponse<JobResponse>> updateJob(
				@PathVariable Long id, @RequestBody JobRequest request) {
			return ResponseEntity.ok(ApiResponse.success(service.updateJob(id, request), "Poste mis à jour avec succès."));
		}

		@DeleteMapping("/jobs/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
			service.deleteJob(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Poste supprimé avec succès."));
		}

		@GetMapping("/contract-types")
		public ResponseEntity<ApiResponse<List<ContractType>>> getContractTypes() {
			return ResponseEntity.ok(ApiResponse.success(service.getContractTypes()));
		}

		@PostMapping("/contract-types")
		public ResponseEntity<ApiResponse<ContractType>> addContractType(@RequestBody NameRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addContractType(request), "Type de contrat créé avec succès."));
		}

		@PutMapping("/contract-types/{id}")
		public ResponseEntity<ApiResponse<ContractType>> updateContractType(
				@PathVariable Long id, @RequestBody NameRequest request) {
			return ResponseEntity.ok(ApiResponse.success(
					service.updateContractType(id, request), "Type de contrat mis à jour avec succès."));
		}

		@DeleteMapping("/contract-types/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteContractType(@PathVariable Long id) {
			service.deleteContractType(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Type de contrat supprimé avec succès."));
		}

		@GetMapping("/document-types")
		public ResponseEntity<ApiResponse<List<DocumentType>>> getDocumentTypes() {
			return ResponseEntity.ok(ApiResponse.success(service.getDocumentTypes()));
		}

		@PostMapping("/document-types")
		public ResponseEntity<ApiResponse<DocumentType>> addDocumentType(@RequestBody NameRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addDocumentType(request), "Type de document créé avec succès."));
		}

		@PutMapping("/document-types/{id}")
		public ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(
				@PathVariable Long id, @RequestBody NameRequest request) {
			return ResponseEntity.ok(ApiResponse.success(
					service.updateDocumentType(id, request), "Type de document mis à jour avec succès."));
		}

		@DeleteMapping("/document-types/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable Long id) {
			service.deleteDocumentType(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Type de document supprimé avec succès."));
		}

		@GetMapping("/leave-types")
		public ResponseEntity<ApiResponse<List<LeaveType>>> getLeaveTypes() {
			return ResponseEntity.ok(ApiResponse.success(service.getLeaveTypes()));
		}

		@PostMapping("/leave-types")
		public ResponseEntity<ApiResponse<LeaveType>> addLeaveType(@RequestBody NameRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addLeaveType(request), "Type de congé créé avec succès."));
		}

		@PutMapping("/leave-types/{id}")
		public ResponseEntity<ApiResponse<LeaveType>> updateLeaveType(
				@PathVariable Long id, @RequestBody NameRequest request) {
			return ResponseEntity.ok(ApiResponse.success(
					service.updateLeaveType(id, request), "Type de congé mis à jour avec succès."));
		}

		@DeleteMapping("/leave-types/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteLeaveType(@PathVariable Long id) {
			service.deleteLeaveType(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Type de congé supprimé avec succès."));
		}

		@GetMapping("/employees")
		public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployees() {
			return ResponseEntity.ok(ApiResponse.success(service.getEmployees()));
		}

		@GetMapping("/employees/photos")
		public ResponseEntity<ApiResponse<List<PhotoResponse>>> getEmployeePhotos() {
			return ResponseEntity.ok(ApiResponse.success(photoService.listAll(PhotoItemType.EMPLOYEE)));
		}

		@GetMapping("/employees/{id}")
		public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
			return ResponseEntity.ok(ApiResponse.success(service.getEmployee(id)));
		}

		@PostMapping("/employees")
		public ResponseEntity<ApiResponse<EmployeeResponse>> addEmployee(@RequestBody EmployeeRequest request) {
			EmployeeResponse created = service.getEmployee(service.addEmployee(request).getIdEmployee());
			return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Employé créé avec succès."));
		}

		@PutMapping("/employees/{id}")
		public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
			service.updateEmployee(id, request);
			return ResponseEntity.ok(ApiResponse.success(service.getEmployee(id), "Employé mis à jour avec succès."));
		}

		@DeleteMapping("/employees/{id}")
		public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) throws IOException {
			service.getEmployeeEntity(id);
			photoService.deleteAllFor(PhotoItemType.EMPLOYEE, id);
			service.deleteEmployee(id);
			return ResponseEntity.ok(ApiResponse.success(null, "Employé supprimé avec succès."));
		}

		@GetMapping("/documents")
		public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments() {
			return ResponseEntity.ok(ApiResponse.success(service.getDocuments()));
		}

		@PostMapping("/documents")
		public ResponseEntity<ApiResponse<DocumentResponse>> addDocument(@RequestBody DocumentRequest request) {
			var document = service.addDocument(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
					service.getDocuments().stream().filter(item -> item.id().equals(document.getIdDocument())).findFirst().orElseThrow(),
					"Document ajouté avec succès."));
		}

		@GetMapping("/employees/{id}/photos")
		public ResponseEntity<ApiResponse<List<PhotoResponse>>> employeePhotos(@PathVariable Long id) {
			return ResponseEntity.ok(ApiResponse.success(photoService.listByItem(PhotoItemType.EMPLOYEE, id)));
		}

		@PostMapping(value = "/employees/{id}/photos", consumes = "multipart/form-data")
		public ResponseEntity<ApiResponse<PhotoResponse>> uploadEmployeePhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file,
				@RequestParam(required = false) String caption, @RequestParam(required = false) String altText,
				@RequestParam(defaultValue = "true") boolean isCover) throws IOException {
			return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
					photoService.upload(PhotoItemType.EMPLOYEE, id, file, caption, altText, isCover)));
		}
	}
