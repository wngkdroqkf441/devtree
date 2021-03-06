package com.rootnode.devtree.api.response;

import com.rootnode.devtree.db.entity.Team;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudyCreateResponseDto extends  CommonResponseDto{
    private Long teamSeq;
    private String teamName;

    public StudyCreateResponseDto(Team team) {
        this.teamSeq = team.getTeamSeq();
        this.teamName = team.getTeamName();
        this.status = 201;
        this.message = "스터디 생성에 성공하셨습니다.";
    }
}
