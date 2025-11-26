/**
 * 검색 관련 유틸리티 함수
 */

/**
 * 디바운스 함수 - 연속된 함수 호출을 지연시킵니다.
 * @param {Function} func 실행할 함수
 * @param {number} wait 대기 시간 (ms)
 * @returns {Function} 디바운스된 함수
 */
export function debounce(func, wait = 300) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * 검색어를 하이라이트하기 위한 부분 정보를 반환합니다.
 * @param {string} text 원본 텍스트
 * @param {string} searchTerm 검색어
 * @returns {Array<{text: string, highlight: boolean}>} 하이라이트 정보 배열
 */
export function getHighlightParts(text, searchTerm) {
  if (!text || !searchTerm) {
    return [{ text: text || '', highlight: false }];
  }

  const regex = new RegExp(`(${escapeRegex(searchTerm)})`, 'gi');
  const parts = text.split(regex);

  return parts
    .filter(part => part.length > 0)
    .map(part => ({
      text: part,
      highlight: regex.test(part)
    }));
}

/**
 * 정규식 특수문자를 이스케이프합니다.
 * @param {string} str 이스케이프할 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * 검색어를 토큰화합니다.
 * @param {string} searchText 검색어
 * @returns {string[]} 토큰 배열
 */
export function tokenizeSearchText(searchText) {
  if (!searchText) {
    return [];
  }
  
  return searchText
    .trim()
    .split(/\s+/)
    .filter(token => token.length > 0);
}

/**
 * 검색어를 정규화합니다.
 * @param {string} searchText 검색어
 * @returns {string} 정규화된 검색어
 */
export function normalizeSearchText(searchText) {
  if (!searchText) {
    return '';
  }
  
  return searchText
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

/**
 * 검색어 자동완성 제안을 생성합니다.
 * @param {string} searchText 검색어
 * @param {string[]} suggestions 제안 목록
 * @returns {string[]} 필터링된 제안 목록
 */
export function getSearchSuggestions(searchText, suggestions) {
  if (!searchText || !suggestions || suggestions.length === 0) {
    return [];
  }
  
  const normalized = normalizeSearchText(searchText);
  
  return suggestions
    .filter(suggestion => 
      normalizeSearchText(suggestion).includes(normalized)
    )
    .slice(0, 5); // 최대 5개만 반환
}

/**
 * 로컬 스토리지에서 검색 히스토리를 가져옵니다.
 * @returns {string[]} 검색 히스토리 배열
 */
export function getSearchHistory() {
  try {
    const history = localStorage.getItem('searchHistory');
    return history ? JSON.parse(history) : [];
  } catch (e) {
    return [];
  }
}

/**
 * 검색 히스토리에 검색어를 추가합니다.
 * @param {string} searchTerm 검색어
 * @param {number} maxHistory 최대 히스토리 개수 (기본: 10)
 */
export function addToSearchHistory(searchTerm, maxHistory = 10) {
  if (!searchTerm || searchTerm.trim().length === 0) {
    return;
  }
  
  try {
    let history = getSearchHistory();
    
    // 중복 제거
    history = history.filter(term => term !== searchTerm);
    
    // 맨 앞에 추가
    history.unshift(searchTerm.trim());
    
    // 최대 개수 제한
    if (history.length > maxHistory) {
      history = history.slice(0, maxHistory);
    }
    
    localStorage.setItem('searchHistory', JSON.stringify(history));
  } catch (e) {
    console.error('검색 히스토리 저장 실패:', e);
  }
}

/**
 * 검색 히스토리를 삭제합니다.
 */
export function clearSearchHistory() {
  try {
    localStorage.removeItem('searchHistory');
  } catch (e) {
    console.error('검색 히스토리 삭제 실패:', e);
  }
}

