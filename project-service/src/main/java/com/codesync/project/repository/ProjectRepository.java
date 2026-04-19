package com.codesync.project.repository;

import com.codesync.project.entity.Project;
import com.codesync.project.enums.Visibility;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByOwnerId(Long ownerId);

	List<Project> findByOwnerId(Long ownerId, Sort sort);

	Optional<Project> findByProjectId(Long projectId);

	List<Project> findByVisibility(Visibility visibility);

	List<Project> findByVisibilityAndIsArchivedFalse(Visibility visibility);

	List<Project> findByVisibilityAndIsArchivedFalse(Visibility visibility, Sort sort);

	List<Project> findByLanguageIgnoreCase(String language);

	List<Project> findByLanguageIgnoreCaseAndVisibilityAndIsArchivedFalse(String language, Visibility visibility,
			Sort sort);

	@Query("select p from Project p where lower(p.name) like lower(concat('%', :name, '%'))")
	List<Project> searchByName(@Param("name") String name);

	@Query("""
			select p
			from Project p
			where p.visibility = :visibility
			  and p.isArchived = false
			  and lower(p.name) like lower(concat('%', :name, '%'))
			order by p.starCount desc, p.forkCount desc, p.updatedAt desc
			""")
	List<Project> searchDiscoverableByName(@Param("name") String name, @Param("visibility") Visibility visibility);

	@Query("select distinct p from Project p join p.memberUserIds memberUserId where memberUserId = :userId")
	List<Project> findByMemberUserId(@Param("userId") Long userId);

	@Query("""
			select distinct p
			from Project p
			join p.memberUserIds memberUserId
			where memberUserId = :userId
			order by p.updatedAt desc
			""")
	List<Project> findByMemberUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

	List<Project> findByIsArchived(boolean isArchived);

	long countByOwnerId(Long ownerId);
}
