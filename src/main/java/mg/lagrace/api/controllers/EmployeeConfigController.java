package mg.lagrace.api.controllers;

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
	import org.springframework.web.bind.annotation.RestController;

	import mg.lagrace.api.dto.ApiResponse;
	import mg.lagrace.api.dto.employee.JobRequest;
	import mg.lagrace.api.dto.employee.NameRequest;
	import mg.lagrace.api.models.ContractType;
	import mg.lagrace.api.models.Department;
	import mg.lagrace.api.models.DocumentType;
	import mg.lagrace.api.models.Job;
	import mg.lagrace.api.models.LeaveType;
import mg.lagrace.api.services.EmployeeConfigService;


	@RestController
	@RequestMapping("/employee-configs")
	public class EmployeeConfigController {
		private final EmployeeConfigService service;

		public EmployeeConfigController(EmployeeConfigService service) {
			this.service = service;
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
		public ResponseEntity<ApiResponse<List<Job>>> getJobs() {
			return ResponseEntity.ok(ApiResponse.success(service.getJobs()));
		}

		@PostMapping("/jobs")
		public ResponseEntity<ApiResponse<Job>> addJob(@RequestBody JobRequest request) {
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(ApiResponse.success(service.addJob(request), "Poste créé avec succès."));
		}

		@PutMapping("/jobs/{id}")
		public ResponseEntity<ApiResponse<Job>> updateJob(
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
	}
