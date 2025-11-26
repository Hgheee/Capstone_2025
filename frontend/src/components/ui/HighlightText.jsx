import { getHighlightParts } from "../../utils/searchUtils";

/**
 * 검색어를 하이라이트하는 컴포넌트
 * @param {Object} props
 * @param {string} props.text 원본 텍스트
 * @param {string} props.searchTerm 검색어
 * @returns {JSX.Element} 하이라이트된 텍스트
 */
export default function HighlightText({ text, searchTerm }) {
  if (!text || !searchTerm) {
    return <>{text}</>;
  }

  const parts = getHighlightParts(text, searchTerm);

  return (
    <>
      {parts.map((part, index) => {
        if (part.highlight) {
          return (
            <mark key={index} className="bg-yellow-200 font-semibold">
              {part.text}
            </mark>
          );
        }
        return <span key={index}>{part.text}</span>;
      })}
    </>
  );
}


