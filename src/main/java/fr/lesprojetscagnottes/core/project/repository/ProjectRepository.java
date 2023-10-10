package fr.lesprojetscagnottes.core.project.repository;

import fr.lesprojetscagnottes.core.project.entity.ProjectEntity;
import fr.lesprojetscagnottes.core.project.model.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.Set;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    Page<ProjectEntity> findAll(Pageable pageable);

    Set<ProjectEntity> findAllByLeaderId(Long memberId);

    Set<ProjectEntity> findAllByPeopleGivingTime_Id(Long memberId);

    Set<ProjectEntity> findAllByStatusInAndLastStatusUpdateLessThan(Set<ProjectStatus> status, Date lastStatusUpdate);

    Page<ProjectEntity> findAllByStatusIn(Set<ProjectStatus> status, Pageable pageable);

    Page<ProjectEntity> findAllByOrganizationIdAndStatusIn(Long id, Set<ProjectStatus> status, Pageable pageable);

    @Query(
            value = "select p.* from votes v right outer join projects p on p.id = v.project_id where p.status = 'IDEA' group by p.id order by count(v.*) asc limit 1",
            nativeQuery = true)
    ProjectEntity findLessVotedIdea();

}
