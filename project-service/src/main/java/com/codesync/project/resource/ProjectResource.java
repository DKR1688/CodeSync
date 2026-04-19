package com.codesync.project.resource;

import com.codesync.project.dto.ProjectDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import com.codesync.project.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/projects")
public class ProjectResource {

    private final ProjectService service;

    public ProjectResource(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(dto));
    }

    @GetMapping("/{id}")
    public ProjectDTO getProjectById(@PathVariable Long id) {
        return service.getProjectById(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<ProjectDTO> getProjectsByOwner(@PathVariable Long ownerId) {
        return service.getProjectsByOwner(ownerId);
    }

    @GetMapping("/public")
    public List<ProjectDTO> getPublicProjects() {
        return service.getPublicProjects();
    }

    @GetMapping("/search")
    public List<ProjectDTO> searchProjects(@RequestParam String name) {
        return service.searchProjects(name);
    }

    @GetMapping("/member/{userId}")
    public List<ProjectDTO> getProjectsByMember(@PathVariable Long userId) {
        return service.getProjectsByMember(userId);
    }

    @GetMapping("/language/{lang}")
    public List<ProjectDTO> getProjectsByLanguage(@PathVariable String lang) {
        return service.getProjectsByLanguage(lang);
    }

    @PutMapping("/{id}")
    public ProjectDTO updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto) {
        return service.updateProject(id, dto);
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archiveProject(@PathVariable Long id) {
        service.archiveProject(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<Void> starProject(@PathVariable Long id) {
        service.starProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fork/{userId}")
    public ResponseEntity<ProjectDTO> forkProject(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.forkProject(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
