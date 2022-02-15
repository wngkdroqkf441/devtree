import React, { useState, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';
//
import { Button } from '@mui/material';
//
// import { StudyResponseForm } from '../../components/_dashboard/study';
import { getStudyResponse, answerStudyResponse } from '../../_actions/study_actions';

export default function StudyResponse() {
  // STATE
  const teamSeq = useParams().id;
  const [studyRequest, setstudyRequest] = useState([]);
  const [loading, setLoading] = useState(false);

  // AXIOS
  const dispatch = useDispatch();
  // 해당 스터디 정보 불러오기
  const getStyResponse = async () => {
    setLoading(true);
    await dispatch(getStudyResponse(teamSeq))
      .then((response) => {
        console.log(response.payload.data.data, '스터디 요청 목록 불러오기 성공');
        setstudyRequest(response.payload.data.data);
      })
      .catch((error) => {
        console.log(error, '스터디 요청 목록 불러오기 실패');
      });
    setLoading(false);
  };

  // HANDLE
  const sendResponse = (event) => {
    const answer = event.target.value;
    const dataToSubmit = {
      responseType: answer
    };
    const sendAnswer = async () => {
      await dispatch(answerStudyResponse({ teamSeq, dataToSubmit }))
        .then((response) => {
          console.log(response.payload.data.data, '스터디 신청 응답 성공');
        })
        .catch((error) => {
          console.log(error, '스터디 신청 응답 실패');
        });
    };
    sendAnswer();
    getStyResponse();
  };

  // RENDER
  useEffect(() => {
    getStyResponse();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // LOADING
  if (loading) return <div>로딩 중</div>;

  // PAGE
  if (studyRequest.length === 0) return <div>요청이 없음</div>;
  return (
    <div>
      <h1>스터디 신청 목록</h1>
      {studyRequest.map((request) => (
        <div key={request.userSeq}>
          {request.userId}의 신청
          <Button value="ACCEPT" onClick={sendResponse}>
            수락
          </Button>
          <Button value="REJECT" onClick={sendResponse}>
            거절
          </Button>
        </div>
      ))}
    </div>
  );
}
