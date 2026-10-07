import React from 'react';
import FindingSuggestion from './FindingSuggestion';

export const FindingCodeBlock = ({
  suggestion,
  lineNumber,
  endLineNumber,
  filePath
}) => {
  return (
    <FindingSuggestion
      suggestion={suggestion}
      lineNumber={lineNumber}
      endLineNumber={endLineNumber}
    />
  );
};

export default FindingCodeBlock;
