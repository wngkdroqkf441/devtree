package com.rootnode.devtree.api.service;

import com.rootnode.devtree.api.request.*;
import com.rootnode.devtree.api.response.*;
import com.rootnode.devtree.db.entity.*;
import com.rootnode.devtree.db.entity.compositeKey.MentorScheduleId;
import com.rootnode.devtree.db.entity.compositeKey.MentorTechId;
import com.rootnode.devtree.db.entity.compositeKey.TeamTechId;
import com.rootnode.devtree.db.repository.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MentorService {
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final MentorTechRepository mentorTechRepository;
    private final MentoringRepository mentoringRepository;
    private final MentoringCommentRepository mentoringCommentRepository;
    private final MentorScheduleRepository mentorScheduleRepository;
    private final TeamRepository teamRepository;
    private final TechRepository techRepository;
    private final TierRepository tierRepository;
    private final NotificationRepository notificationRepository;

    /**
     *  mentorlist pagination
     */
//    public Page<MentorListResponseDto> findMentors(Pageable pageable) {
//        Page<Mentor> mentors = mentorRepository.findAllWithPagination(pageable);
//        return new PageImpl(mentors.stream()
//                .map(mentor -> {
//                    List<MentorTechInfoDto> mentorTechInfoDtoList =
//                            mentorTechRepository.findByMentorTechIdMentorSeq(mentor.getMentorSeq()).stream()
//                                    .map(mentorTech -> new MentorTechInfoDto(mentorTech))
//                                    .collect(Collectors.toList());
//
//                    return new MentorListResponseDto(mentor, mentorTechInfoDtoList);
//                })
//                .collect(Collectors.toList()));
//    }

    /**
     * mentorlist non pagination
     */
    public List<MentorListResponseDto> findMentors() {
        List<Mentor> mentors = mentorRepository.findAll();
        return mentors.stream()
                .map(mentor -> {
                    List<MentorTechInfoDto> mentorTechInfoDtoList =
                            mentorTechRepository.findByMentorTechIdMentorSeq(mentor.getMentorSeq()).stream()
                                    .map(mentorTech -> new MentorTechInfoDto(mentorTech))
                                    .collect(Collectors.toList());
                    Tier tier = tierRepository.findByTierMaxExpGreaterThanEqualAndTierMinExpLessThanEqual(mentor.getMentorExp(), mentor.getMentorExp());
                    return new MentorListResponseDto(mentor, tier, mentorTechInfoDtoList);
                })
                .collect(Collectors.toList());

    }

    /**
     * sorted mentorlist non pagination
     */
    public List<MentorSortedListResponseDto> findSortedMentors() {
        AtomicLong index = new AtomicLong();
        List<Mentor> mentors = mentorRepository.findAll(Sort.by(Sort.Direction.DESC, "mentorExp"));
        return mentors.stream()
                .map(mentor -> {
                    List<MentorTechInfoDto> mentorTechInfoDtoList =
                            mentorTechRepository.findByMentorTechIdMentorSeq(mentor.getMentorSeq()).stream()
                                    .map(mentorTech -> new MentorTechInfoDto(mentorTech))
                                    .collect(Collectors.toList());
                    Tier tier = tierRepository.findByTierMaxExpGreaterThanEqualAndTierMinExpLessThanEqual(mentor.getMentorExp(), mentor.getMentorExp());
                    return new MentorSortedListResponseDto(mentor, index.getAndIncrement(), tier, mentorTechInfoDtoList);
                })
                .collect(Collectors.toList());
    }

    public List<MentorListResponseDto> findTechMentors(MentorTechRequestDto requestDto) {
        List<Long> mentorTechSeq = requestDto.getMentorTechSeq();
        Map<Long, MentorListResponseDto> mentorListMap = new HashMap<>();

        mentorTechSeq.forEach(techSeq -> {
            List<Mentor> mentors = mentorTechRepository.findByTechSeq(techSeq);
            mentors.forEach(mentor -> {
                Long mentorSeq = mentor.getMentorSeq();
                Long mentorExp = mentor.getMentorExp();
                Tier tier = tierRepository.findByTierMaxExpGreaterThanEqualAndTierMinExpLessThanEqual(mentorExp, mentorExp);
                List<MentorTechInfoDto> mentorTechList = mentorTechRepository.findByMentorTechIdMentorSeq(mentorSeq).stream()
                        .map(mentorTech -> new MentorTechInfoDto(mentorTech)).collect(Collectors.toList());
                if (mentorListMap.get(mentorSeq) == null) {
                    mentorListMap.put(mentorSeq, new MentorListResponseDto(mentor, tier, mentorTechList));
                }
            });
        });
        return mentorListMap.values().stream().collect(Collectors.toList());
    }

    /**
     * ?????? ?????? ?????? ?????????
     */
    public MentorDetailResponseDto findMentor(Long mentorSeq) {
        // 1. mentor ??????
        Mentor mentor = mentorRepository.findById(mentorSeq).get();

        // 2. user??? ?????? mentor nickname + email ??????
        User user = userRepository.findById(mentorSeq).get();

        // 3. ?????? ?????? ????????? ??????
        List<MentorTechInfoDto> mentorTechInfoDtoList = mentorTechRepository.findByMentorTechIdMentorSeq(mentorSeq).stream()
                .map(mentorTech -> new MentorTechInfoDto(mentorTech))
                .collect(Collectors.toList());

        // 4. ????????? ?????? ?????? (??? ???????????? ?????? ?????????) -> ???????????? ??????(????????? ??? ?????? ????????????, ????????? ?????? ??? ??????)
        List<Mentoring> mentoringList = mentoringRepository.findByMentorMentorSeqAndMentoringState(mentorSeq, MentoringState.FINISH);

        List<MentoringInfoDto> mentoringInfoList = new ArrayList<>();
        mentoringList.forEach(mentoring -> {
            Team team = mentoring.getTeam();
            List<TeamTech> teamTechList = team.getTeamTechList();
            LocalDate mentoringStartDate = mentoring.getMentoringStartDate();
            LocalTime mentoringStartTime = mentoring.getMentoringStartTime();

            List<String> techNameList = teamTechList.stream()
                    .map(teamTech -> {
                        return teamTech.getTech().getTechName();
                    })
                    .collect(Collectors.toList());

            mentoringInfoList.add(new MentoringInfoDto(team.getTeamName(), techNameList, mentoringStartDate, mentoringStartTime));
        });

        // 5. ????????? ?????? ??????
        List<MentoringCommentInfoDto> reviewDtoList = new ArrayList<>();
        mentoringList.forEach(mentoring -> {
            Long mentoringSeq = mentoring.getMentoringSeq();

            List<MentoringComment> mentoringCommentList = mentoringCommentRepository.findByMentoringSeq(mentoringSeq);
            mentoringCommentList.forEach(mentoringComment -> {
                reviewDtoList.add(new MentoringCommentInfoDto(userRepository.findByUserSeq(mentoringComment.getUser().getUserSeq()).get(), mentoringComment));
            });

        });
        // 6. ?????? ??????
        Tier tier = tierRepository.findByTierMaxExpGreaterThanEqualAndTierMinExpLessThanEqual(mentor.getMentorExp(), mentor.getMentorExp());

        return MentorDetailResponseDto.builder()
                .mentorCareer(mentor.getMentorCareer())
                .mentorDesc(mentor.getMentorDesc())
                .mentorEmail(user.getUserEmail())
                .mentorNickname(user.getUserNickname())
                .mentorTechList(mentorTechInfoDtoList)
                .mentoringInfoList(mentoringInfoList)
                .mentoringReviewList(reviewDtoList)
                .mentorExp(mentor.getMentorExp())
                .tier(tier)
                .build();
    }

    /**
     * ?????? ?????? ?????? ?????????
     */
    public MentorSelfDetailSelfResponseDto findMentorSelf(Long mentorSeq) {
        // 1. mentor ??????
        Mentor mentor = mentorRepository.findById(mentorSeq).get();

        // 2. user??? ?????? mentor name, nickname, career, email ??????
        User user = userRepository.findById(mentorSeq).get();

        // 3. ?????? ?????? ????????? ??????
        List<MentorTechInfoDto> mentorTechInfoDtoList = mentorTechRepository.findByMentorTechIdMentorSeq(mentorSeq).stream()
                .map(mentorTech -> new MentorTechInfoDto(mentorTech))
                .collect(Collectors.toList());

        // 4. ????????? ?????? ?????? (??? ???????????? ?????? ?????????) -> ???????????? ??????(????????? ??? ?????? ????????????, ????????? ?????? ??? ??????)
        List<Mentoring> mentoringList = mentoringRepository.findByMentorMentorSeqAndMentoringState(mentorSeq, MentoringState.FINISH);

        List<MentoringInfoDto> mentoringInfoList = new ArrayList<>();
        mentoringList.forEach(mentoring -> {
            Team team = mentoring.getTeam();
            List<TeamTech> teamTechList = team.getTeamTechList();
            LocalDate mentoringStartDate = mentoring.getMentoringStartDate();
            LocalTime mentoringStartTime = mentoring.getMentoringStartTime();

            List<String> techNameList = teamTechList.stream()
                    .map(teamTech -> {
                        return teamTech.getTech().getTechName();
                    })
                    .collect(Collectors.toList());

            mentoringInfoList.add(new MentoringInfoDto(team.getTeamName(), techNameList, mentoringStartDate, mentoringStartTime));
        });

        // 5. ????????? ?????? ??????
        List<MentoringCommentInfoDto> reviewDtoList = new ArrayList<>();
        mentoringList.forEach(mentoring -> {
            Long mentoringSeq = mentoring.getMentoringSeq();

            List<MentoringComment> mentoringCommentList = mentoringCommentRepository.findByMentoringSeq(mentoringSeq);
            mentoringCommentList.forEach(mentoringComment -> {
                reviewDtoList.add(new MentoringCommentInfoDto(userRepository.findById(mentoringComment.getUser().getUserSeq()).get(), mentoringComment));
            });

        });

        // 6. ????????? ?????? ?????? ??????
        // ?????? ??? ????????? ?????? ??????
        // ????????? ?????? ?????? ?????? api??? ??????????????? ???
//        LocalDate currentDate = LocalDate.now();  // ????????? ????????? ?????? ?????????????????? ???????????? ????????? ????????????....
//        List<MentorScheduleId> mentorTimeList = mentorScheduleRepository.findAfterNowByMentorSeq(mentorSeq, currentDate);
//
//        List<MentorTimeInfoDto> availableTimeList = new ArrayList<>();
//
//        mentorTimeList.forEach(schedule -> {
//            // false??? ??????, true??? ??????
//            boolean exist = false;
//            LocalDate date = schedule.getMentorDate();
//            LocalTime time = schedule.getMentorTime();
//            for (MentorTimeInfoDto availableTime : availableTimeList) {
//                if(availableTime.getMentorDate().equals(date)) {
//                    availableTime.getMentorTime().add(time);
//                    exist = true;
//                    break;
//                }
//            }
//            if(!exist) {
//                List<LocalTime> timeList = new ArrayList<>();
//                timeList.add(time);
//                availableTimeList.add(new MentorTimeInfoDto(date, timeList));
//            }
//        });
//        availableTimeList.stream().sorted(Comparator.comparing(MentorTimeInfoDto::getMentorDate)).collect(Collectors.toList());
//        availableTimeList.forEach(availableTime->{
//            List<LocalTime> timeList = availableTime.getMentorTime();
//            Collections.sort(timeList);
//        });

        return MentorSelfDetailSelfResponseDto.builder()
                .mentorName(user.getUserName())
                .mentorCareer(mentor.getMentorCareer())
                .mentorDesc(mentor.getMentorDesc())
                .mentorEmail(user.getUserEmail())
                .mentorNickname(user.getUserNickname())
                .mentorTechList(mentorTechInfoDtoList)
                .mentoringInfoList(mentoringInfoList)
                .mentoringReviewList(reviewDtoList)
                .mentorExp(mentor.getMentorExp())
                .tier(tierRepository.findByTierMaxExpGreaterThanEqualAndTierMinExpLessThanEqual(mentor.getMentorSeq(), mentor.getMentorExp()))
                .build();
    }


    // ?????? ?????? ??????
    @Transactional
    public CommonResponseDto updateMentor(Long mentorSeq, MentorUpdateRequestDto requestDto) {
        // ?????? ??????, ?????? ??????, ?????? ?????? ?????? ??????
        Mentor mentor = mentorRepository.findById(mentorSeq).get();
        User user = userRepository.findById(mentorSeq).get();

        // ?????? ????????? == ?????? ????????? ??????
        if (StringUtils.hasText(requestDto.getMentorNickName())) {
            user.changeUserNickName(requestDto.getMentorNickName());
        }

        // ?????? ????????? ??????
        if (StringUtils.hasText(requestDto.getMentorCareer())) {
            mentor.changeMentorCareer(requestDto.getMentorCareer());
        }

        // ?????? ?????? ??????
        if (StringUtils.hasText(requestDto.getMentorDesc())) {
            mentor.changeMentorDesc(requestDto.getMentorDesc());
        }

        // ?????? ????????? == ?????? ????????? ??????
        if (StringUtils.hasText(requestDto.getMentorEmail())) {
            user.changeUserEmail(requestDto.getMentorEmail());
        }

        // ?????? ?????? ?????? ??????
        if (!Objects.isNull(requestDto.getMentorTech())) {
            // ??????
            mentorTechRepository.deleteByMentorSeq(mentorSeq);

            // ?????? ??????
            requestDto.getMentorTech().forEach(techSeq -> {
                mentorTechRepository.save(MentorTech.builder()
                        .mentorTechId(new MentorTechId(mentorSeq, techSeq))
                        .mentor(mentor)
                        .tech(techRepository.findById(techSeq).get())
                        .build());
            });
        }
        return new CommonResponseDto(201, "?????? ?????? ????????? ?????????????????????.");
    }

    public CommonResponseDto changeSchedule(Long mentorSeq, MentorScheduleRequestDto requestDto) {
        Mentor mentor = mentorRepository.findById(mentorSeq).get();
        LocalDate mentorDate = requestDto.getMentorDate();
        List<LocalTime> mentorTimeList = requestDto.getMentorTime();

        mentorScheduleRepository.deleteByMentorSeqAndDate(mentorSeq, mentorDate);

        mentorTimeList.forEach(mentorTime -> {
            mentorScheduleRepository.save(new MentorSchedule(new MentorScheduleId(mentorDate, mentorTime, mentorSeq), mentor));
        });
        return new CommonResponseDto(201, "????????? ????????? ?????????????????????.");
    }

    // ????????? ?????? ????????? ??????
    public List<String> findAvailableTime(Long mentorSeq, MentoringAvailableTimeRequestDto requestDto) {
        LocalDate selectedDate = requestDto.getMentorDate();

        List<LocalTime> availableTimeList = mentorScheduleRepository.findByMentorSeqAndDate(mentorSeq, selectedDate);
        return availableTimeList.stream()
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm"))).collect(Collectors.toList());
    }

    // ?????? ????????? ????????? ????????? ??????
    public List<String> findUnavailableTime(Long mentorSeq, MentoringAvailableTimeRequestDto requestDto) {
        LocalDate selectedDate = requestDto.getMentorDate();

        // ????????? ?????? ??????
        List<Mentoring> mentoringList = mentoringRepository.findByMentorMentorSeqAndMentoringState(mentorSeq, MentoringState.ACCEPT)
                .stream().filter(mentoring -> mentoring.getMentoringStartDate().equals(selectedDate)).collect(Collectors.toList());

        return mentoringList.stream()
                .map(mentoring -> mentoring.getMentoringStartTime().format(DateTimeFormatter.ofPattern(("HH:mm"))))
                .collect(Collectors.toList());
    }

    // ????????? ??????
    public CommonResponseDto applyMentoring(MentoringApplyRequestDto requestDto) {
        Team team = teamRepository.findById(requestDto.getTeamSeq()).get();
        Mentor mentor = mentorRepository.findById(requestDto.getMentorSeq()).get();
        mentoringRepository.save(requestDto.toEntity(team, mentor));

        // ????????? ?????? ?????? ?????????
        // ?????? ??????
        String content = team.getTeamName() + "???(" + team.getTeamType() + ")??? ????????? ?????? ????????? ???????????????";
        //sendUserSeq, receiveUserSeq, teamSeq, sendTime, content, notificationType
        Notification notification = new Notification(team.getTeamManagerSeq(), mentor.getMentorSeq(), team.getTeamSeq(),
                LocalDateTime.now(), content, NotificationType.MENTORING);
        // 5. ?????? ???????????? ??????
        notificationRepository.save(notification);

        return new CommonResponseDto(201, "????????? ????????? ?????????????????????.");
    }

    // ????????? ????????? ?????? ?????? ??????
    public List<MentoringApplyListResponseDto> findMentoringApplyList(Long mentorSeq) {
        // ????????? ?????? ??????
        List<Mentoring> mentoringList = mentoringRepository.findByMentorSeq(mentorSeq);
        // ????????? ?????? ?????? ??? ?????? -> ?????? ?????? ??? ?????? -> ?????? ?????? ??? ??????
        return mentoringList.stream()
                .map(mentoring -> new MentoringApplyListResponseDto(mentoring))
                .sorted(Comparator.comparing(MentoringApplyListResponseDto::getMentoringStartDate).thenComparing(MentoringApplyListResponseDto::getMentoringStartTime).thenComparing(MentoringApplyListResponseDto::getMentoringCreateTime))
                .collect(Collectors.toList());
    }


    // ????????? ????????? ????????? ?????? ??????
    public List<MentoringApplyListResponseDto> findMentoringApplyAcceptList(Long mentorSeq) {
        // ????????? ?????? ??????
        List<Mentoring> mentoringList = mentoringRepository.findByMentorMentorSeqAndMentoringState(mentorSeq, MentoringState.ACCEPT);
        // ????????? ?????? ?????? ??? ?????? -> ?????? ?????? ??? ?????? -> ?????? ?????? ??? ??????
        return mentoringList.stream()
                .map(mentoring -> new MentoringApplyListResponseDto(mentoring))
                .sorted(Comparator.comparing(MentoringApplyListResponseDto::getMentoringStartDate).thenComparing(MentoringApplyListResponseDto::getMentoringStartTime).thenComparing(MentoringApplyListResponseDto::getMentoringCreateTime))
                .collect(Collectors.toList());
    }

    // ????????? ??????
    @Transactional
    public CommonResponseDto respondMentoring(Long mentorSeq, Long mentoringSeq, MentoringApplyRespondRequestDto requestDto) {
        ResponseType responseType = requestDto.getResponseType();
        LocalDate mentoringDate = mentoringRepository.findById(mentoringSeq).get().getMentoringStartDate();
        LocalTime mentoringTime = mentoringRepository.findById(mentoringSeq).get().getMentoringStartTime();

        Mentor mentor = mentorRepository.findById(mentorSeq).get();
        Mentoring mentoring = mentoringRepository.findById(mentoringSeq).get();
        Team team = teamRepository.findById(mentoring.getTeam().getTeamSeq()).get();


        // ???????????? ??????
        if (ResponseType.ACCEPT.equals(responseType)) {
            // ????????? ACCEPT?????? ?????????
            mentoringRepository.acceptMentoring(mentoringSeq);
            // ?????? ????????? ??????????????? ??????
            mentorScheduleRepository.deleteByDateAndTime(mentoringDate, mentoringTime);

            int mCount = mentor.getMentoringCnt() + 1;
            //?????? ??????
            mentor.changeMentorCount(mCount);
            //?????? ???
            Team CurTeam = mentoringRepository.findById(mentoringSeq).get().getTeam();
            //????????????
            int Count = mentoringRepository.countByTeamTeamSeqAndMentorMentorSeq(CurTeam.getTeamSeq(), mentorSeq);
            //???????????? ???????????? ???????????? 100, ?????? ???????????? +20 ~ +30 ~ +40  +50
            mentor.changeMentorExp(mCount * 100L + 10 + 10 * Count);

            // ????????? ?????? ?????? ?????????
            // ?????? ??????
            String content = userRepository.findByUserSeq(mentorSeq).get().getUserNickname() + "????????????" + team.getTeamName() + "?????? ????????? ????????? ?????????????????????!";
            //sendUserSeq, receiveUserSeq, teamSeq, sendTime, content, notificationType
            Notification notification = new Notification(mentorSeq, team.getTeamManagerSeq(), team.getTeamSeq(),
                    LocalDateTime.now(), content, NotificationType.MENTORING);
            // 5. ?????? ???????????? ??????
            notificationRepository.save(notification);
        }

        // ???????????? ??????
        if (ResponseType.REJECT.equals(responseType)) {
            // ???????????? ??????
            mentoringRepository.deleteByMentoringSeq(mentoringSeq);

            // ????????? ?????? ?????? ?????????
            // ?????? ??????
            String content = userRepository.findByUserSeq(mentorSeq).get().getUserNickname() + " ???????????? " + team.getTeamName() + " ??? ????????? ????????? ?????????????????????.";
            //sendUserSeq, receiveUserSeq, teamSeq, sendTime, content, notificationType
            Notification notification = new Notification(mentorSeq, team.getTeamManagerSeq(), team.getTeamSeq(),
                    LocalDateTime.now(), content, NotificationType.MENTORING);
            // 5. ?????? ???????????? ??????
            notificationRepository.save(notification);
        }
        return new CommonResponseDto(201, "????????? ?????? ????????? ?????????????????????.");
    }

    @Transactional
    public CommonResponseDto changeMentoringState(Long mentoringSeq) {
        Mentoring mentoring = mentoringRepository.findById(mentoringSeq).get();

        MentoringState[] mentoringStates = MentoringState.values();
        MentoringState currentState = mentoring.getMentoringState();

        // ?????? mentoringState ?????? ????????? ????????? ???????????????.
        int ordinal = currentState.ordinal();
        if (ordinal < mentoringStates.length - 1) {
            MentoringState nextState = mentoringStates[ordinal + 1];
            mentoring.changeMentoringState(nextState);
            return new CommonResponseDto(201, "????????? ?????? ????????? ?????????????????????.");
        }

        return new CommonResponseDto(400, "????????? ?????? ????????? ?????????????????????.");
    }

}
