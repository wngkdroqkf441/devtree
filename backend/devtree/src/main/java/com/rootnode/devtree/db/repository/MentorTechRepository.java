package com.rootnode.devtree.db.repository;

import com.rootnode.devtree.db.entity.MentorTech;
import com.rootnode.devtree.db.entity.compositeKey.MentorTechId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MentorTechRepository extends JpaRepository<MentorTech, MentorTechId> {
    //select ? from ? join ? on id = id
    @Query(value = "select t from MentorTech t join fetch t.tech")
    List<MentorTech> findByMentorTechIdMentorSeq(Long mentorSeq);
}
