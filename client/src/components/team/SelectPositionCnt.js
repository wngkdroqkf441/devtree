import PropTypes from 'prop-types';
import { useState, useCallback } from 'react';
import { FormControl, InputLabel, NativeSelect, Button } from '@mui/material';

SelectPositionCnt.propTypes = {
  onSetCnt: PropTypes.func.isRequired,
  pos: PropTypes.string.isRequired,
  defaultValue: PropTypes.number
};

export default function SelectPositionCnt(props) {
  // STATE
  const MEMBER_SELECT = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
  const [selectedPositionCnt, setselectedPositionCnt] = useState('');
  const [cleared, setCleared] = useState(false);

  // HANDLE
  const handleChange = (e) => {
    setselectedPositionCnt((e.target.value *= 1));
  };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const getData = useCallback(() => {
    props.onSetCnt(selectedPositionCnt, props.pos, props.defaultValue);
    setCleared(true);
  });

  // CONDITIONAL
  if (!cleared) {
    return (
      <FormControl>
        <InputLabel htmlFor="select-position-cnt" />
        <NativeSelect
          label="select-position-cnt"
          defaultValue={props.defaultValue}
          // value={selectedPositionCnt}
          onChange={handleChange}
        >
          {MEMBER_SELECT.map((month, idx) => (
            <option key={idx} value={month}>
              {month}
            </option>
          ))}
        </NativeSelect>
        <Button onClick={getData}>확정</Button>
      </FormControl>
    );
  }
  return (
    <FormControl disabled>
      <InputLabel htmlFor="select-position-cnt" />

      <NativeSelect
        label="select-position-cnt"
        defaultValue={props.defaultValue}
        // value={selectedPositionCnt}
        onChange={handleChange}
      >
        {MEMBER_SELECT.map((month, idx) => (
          <option key={idx} value={month}>
            {month}
          </option>
        ))}
      </NativeSelect>
    </FormControl>
  );

  // PAGE
}
