import { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { lostItemApi } from "../lib/api";
import ItemCard from "../components/ui/ItemCard.jsx";
import { debounce, addToSearchHistory, getSearchHistory } from "../utils/searchUtils";

// 카테고리 옵션 정의
const CATEGORIES = [
  "전체",
  "지갑",
  "가방",
  "핸드폰",
  "노트북",
  "서류",
  "귀중품",
  "의류",
  "우산",
  "도서",
  "기타"
];

// 상태 옵션 정의
const STATUS_OPTIONS = [
  { label: "전체", value: null },
  { label: "습득", value: "FOUND" },
  { label: "보관중", value: "STORED" },
  { label: "수령완료", value: "CLAIMED" },
  { label: "반환완료", value: "RETURNED" },
  { label: "기간만료", value: "EXPIRED" },
];

// 광역시/도별 시/군/구 옵션
const REGION_HIERARCHY = {
  "전체": [],
  "서울": ["강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
           "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
           "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"],
  "경기": ["고양시", "수원시", "성남시", "용인시", "부천시", "안산시", "안양시", "남양주시", "화성시", 
           "평택시", "의정부시", "시흥시", "파주시", "김포시", "광명시", "광주시", "군포시", "하남시",
           "오산시", "양주시", "이천시", "구리시", "안성시", "포천시", "의왕시", "여주시", "양평군",
           "동두천시", "과천시", "가평군", "연천군"],
  "부산": ["강서구", "금정구", "기장군", "남구", "동구", "동래구", "부산진구", "북구",
           "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구"],
  "대구": ["군위군", "남구", "달서구", "달성군", "동구", "북구", "서구", "수성구", "중구"],
  "인천": ["강화군", "계양구", "미추홀구", "남동구", "동구", "부평구", "서구", "연수구", "옹진군", "중구"],
  "광주": ["광산구", "남구", "동구", "북구", "서구"],
  "대전": ["대덕구", "동구", "서구", "유성구", "중구"],
  "울산": ["남구", "동구", "북구", "울주군", "중구"],
  "세종": ["세종시"],
  "강원": ["강릉시", "동해시", "삼척시", "속초시", "원주시", "춘천시", "태백시", "고성군", "양구군",
           "양양군", "영월군", "인제군", "정선군", "철원군", "평창군", "홍천군", "화천군", "횡성군"],
  "충북": ["청주시", "충주시", "제천시", "괴산군", "단양군", "보은군", "영동군", "옥천군", "음성군", "증평군", "진천군"],
  "충남": ["천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시",
           "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"],
  "전북": ["전주시", "군산시", "익산시", "정읍시", "남원시", "김제시",
           "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"],
  "전남": ["목포시", "여수시", "순천시", "나주시", "광양시",
           "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군",
           "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"],
  "경북": ["포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시",
           "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군",
           "칠곡군", "예천군", "봉화군", "울진군", "울릉군"],
  "경남": ["창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시",
           "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"],
  "제주": ["제주시", "서귀포시"]
};

// 광역시/도 목록
const PROVINCES = Object.keys(REGION_HIERARCHY);

export default function Search() {
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("전체");
  const [selectedStatus, setSelectedStatus] = useState(null); // 상태 필터 추가
  const [selectedProvince, setSelectedProvince] = useState("전체");
  const [selectedDistrict, setSelectedDistrict] = useState("전체");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchHistory, setSearchHistory] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // URL 쿼리 파라미터에서 초기 검색어 가져오기
  useEffect(() => {
    const queryParam = searchParams.get("q");
    if (queryParam) {
      setKeyword(queryParam);
    }
  }, [searchParams]);

  // 검색 히스토리 로드
  useEffect(() => {
    setSearchHistory(getSearchHistory());
  }, []);

  // 디바운스된 검색 함수 (실시간 검색용)
  const debouncedSearch = useCallback(
    debounce((searchKeyword, page = 0) => {
      performSearch(searchKeyword, page);
    }, 500), // 500ms 대기
    []
  );

  // 검색 실행 함수
  const performSearch = async (searchKeyword = keyword, page = 0, categoryOverride = null) => {
    setLoading(true);
    setError(null);
    
    // searchKeyword가 문자열이 아니면 문자열로 변환
    const searchText = typeof searchKeyword === 'string' ? searchKeyword : String(searchKeyword || '');
    
    // 검색어가 있으면 히스토리에 추가
    if (searchText && searchText.trim()) {
      addToSearchHistory(searchText.trim());
      setSearchHistory(getSearchHistory());
    }
    
    try {
      let response;
      
      // 고급 검색 (키워드, 카테고리, 지역 조합)
      const searchParams = {
        page,
        size: 12,
        sort: "createdAt,desc"
      };

      // 키워드가 있으면 추가
      if (searchText.trim()) {
        searchParams.keyword = searchText.trim();
      }

      // 카테고리가 선택되었으면 추가 (categoryOverride가 있으면 우선 사용)
      const categoryToUse = categoryOverride !== null ? categoryOverride : selectedCategory;
      if (categoryToUse && categoryToUse !== "전체") {
        searchParams.category = categoryToUse;
      }

      // 상태가 선택되었으면 추가
      if (selectedStatus) {
        searchParams.status = selectedStatus;
      }

      // 지역 검색 처리 (광역시/도 + 시/군/구)
      let regionQuery = null;
      if (selectedProvince !== "전체" && selectedDistrict !== "전체") {
        regionQuery = `${selectedProvince} ${selectedDistrict}`;
      } else if (selectedProvince !== "전체") {
        regionQuery = selectedProvince;
      } else if (selectedDistrict !== "전체") {
        regionQuery = selectedDistrict;
      }

      // 지역 파라미터 추가
      if (regionQuery) {
        searchParams.region = regionQuery;
      }

      // 검색 조건에 따라 적절한 API 선택
      if (regionQuery && !searchText && categoryToUse === "전체" && !selectedStatus) {
        // 지역만 선택된 경우
        response = await lostItemApi.searchByRegion(regionQuery, searchParams);
      } else if (regionQuery || (categoryToUse && categoryToUse !== "전체") || selectedStatus || searchText.trim()) {
        // 지역, 카테고리, 상태, 키워드 중 하나라도 있으면 고급 검색 사용
        response = await lostItemApi.advancedSearch(searchParams);
      } else {
        // 아무 조건도 없으면 전체 목록
        response = await lostItemApi.list(searchParams);
      }

      // 백엔드 ApiResponse 구조: { success, data, error }
      // axios는 이를 response.data에 저장
      if (response.data.success) {
        const pageData = response.data.data;
        // Page 객체 또는 List 객체 처리
        if (pageData.content) {
          // Page 객체인 경우
          setResults(pageData.content || []);
          setTotalPages(pageData.totalPages || 0);
        } else if (Array.isArray(pageData)) {
          // List 객체인 경우
          setResults(pageData);
          setTotalPages(1);
        } else {
          setResults([]);
          setTotalPages(0);
        }
        setCurrentPage(page);
      } else {
        throw new Error(response.data.error?.message || "검색에 실패했습니다.");
      }
    } catch (err) {
      console.error("검색 오류:", err);
      const errorMessage = err.response?.data?.error?.message || 
                          err.message || 
                          "검색 중 오류가 발생했습니다. 다시 시도해주세요.";
      setError(errorMessage);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트 마운트 시 또는 검색어 변경 시 초기 데이터 로드
  useEffect(() => {
    // 실시간 검색: 키워드가 변경되면 디바운스된 검색 실행
    if (keyword.trim()) {
      debouncedSearch(keyword, 0);
    } else {
      // 키워드가 비어있으면 전체 목록
      performSearch("", 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  // 검색 버튼 클릭 또는 폼 제출
  const handleSubmit = (e) => {
    e.preventDefault();
    setCurrentPage(0);
    performSearch(keyword, 0);
  };

  // 페이지 변경
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      performSearch(keyword, newPage); // ✅ keyword와 newPage를 올바르게 전달
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-2">분실물 검색</h2>
        <p className="text-gray-600">지역과 카테고리를 선택하여 분실물을 검색하세요</p>
      </div>

      {/* 검색 폼 */}
      <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg shadow-sm border">
        {/* 키워드 검색 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            키워드 검색
          </label>
          <div className="relative flex gap-2">
            <div className="flex-1 relative">
              <input
                type="text"
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.target.value);
                  setShowSuggestions(true);
                }}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                placeholder="예: 지갑, 우산, 에어팟"
                className="w-full border rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              
              {/* 검색 제안 드롭다운 */}
              {showSuggestions && searchHistory.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-48 overflow-y-auto">
                  {searchHistory
                    .filter(term => term.toLowerCase().includes(keyword.toLowerCase()))
                    .slice(0, 5)
                    .map((term, index) => (
                      <button
                        key={index}
                        type="button"
                        onClick={() => {
                          setKeyword(term);
                          setShowSuggestions(false);
                          performSearch(term, 0);
                        }}
                        className="w-full text-left px-4 py-2 hover:bg-gray-100 transition-colors"
                      >
                        <span className="text-gray-600">🔍</span> {term}
                      </button>
                    ))}
                </div>
              )}
            </div>
            <button
              type="submit"
              className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors font-medium"
            >
              검색
            </button>
          </div>
        </div>

        {/* 카테고리 선택 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            카테고리
          </label>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((category) => (
              <button
                key={category}
                type="button"
                onClick={async () => {
                  setSelectedCategory(category);
                  setCurrentPage(0);
                  // 카테고리 선택 시 자동 검색 실행 (카테고리만 선택해도 검색)
                  // categoryOverride를 사용하여 최신 카테고리 값 전달
                  await performSearch("", 0, category);
                }}
                className={`px-4 py-2 rounded-md border transition-colors ${
                  selectedCategory === category
                    ? "bg-blue-600 text-white border-blue-600"
                    : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
                }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {/* 상태 선택 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            상태
          </label>
          <div className="flex flex-wrap gap-2">
            {STATUS_OPTIONS.map((status) => (
              <button
                key={status.label}
                type="button"
                onClick={async () => {
                  setSelectedStatus(status.value);
                  setCurrentPage(0);
                  // 상태 선택 시 자동 검색 실행
                  await performSearch(keyword || "", 0);
                }}
                className={`px-4 py-2 rounded-md border transition-colors ${
                  selectedStatus === status.value
                    ? "bg-green-600 text-white border-green-600"
                    : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
                }`}
              >
                {status.label}
              </button>
            ))}
          </div>
        </div>

        {/* 지역 선택 */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              광역시/도
            </label>
            <select
              value={selectedProvince}
              onChange={(e) => {
                setSelectedProvince(e.target.value);
                setSelectedDistrict("전체");
                setCurrentPage(0);
              }}
              className="w-full border rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {PROVINCES.map((province) => (
                <option key={province} value={province}>
                  {province}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              시/군/구
            </label>
            <select
              value={selectedDistrict}
              onChange={(e) => {
                setSelectedDistrict(e.target.value);
                setCurrentPage(0);
              }}
              className="w-full border rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={selectedProvince === "전체"}
            >
              <option value="전체">전체</option>
              {selectedProvince !== "전체" && REGION_HIERARCHY[selectedProvince]?.map((district) => (
                <option key={district} value={district}>
                  {district}
                </option>
              ))}
            </select>
          </div>
        </div>
        {(selectedProvince !== "전체" || selectedDistrict !== "전체") && (
          <p className="mt-1 text-xs text-gray-500">
            💡 선택한 지역 및 주변 인접 지역의 분실물이 함께 검색됩니다
          </p>
        )}
      </form>

      {/* 로딩 상태 */}
      {loading && (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">검색 중...</p>
        </div>
      )}

      {/* 에러 메시지 */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
          {error}
        </div>
      )}

      {/* 검색 결과 */}
      {!loading && !error && (
        <>
          <div className="flex justify-between items-center">
            <p className="text-gray-600">
              총 <span className="font-semibold text-blue-600">{results.length}</span>개의 분실물
              {totalPages > 1 && ` (${currentPage + 1} / ${totalPages} 페이지)`}
            </p>
          </div>

          {results.length === 0 ? (
            <div className="text-center py-12 bg-gray-50 rounded-lg">
              <p className="text-gray-500 text-lg">검색 결과가 없습니다.</p>
              <p className="text-gray-400 text-sm mt-2">다른 검색 조건을 시도해보세요.</p>
            </div>
          ) : (
            <>
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {results.map((item) => (
                  <ItemCard key={item.id} item={item} searchKeyword={keyword} />
                ))}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex justify-center items-center gap-2 mt-6">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    이전
                  </button>
                  
                  <div className="flex gap-1">
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      const pageNum = Math.max(0, Math.min(currentPage - 2, totalPages - 5)) + i;
                      if (pageNum >= totalPages) return null;
                      return (
                        <button
                          key={pageNum}
                          onClick={() => handlePageChange(pageNum)}
                          className={`px-4 py-2 border rounded-md ${
                            currentPage === pageNum
                              ? "bg-blue-600 text-white border-blue-600"
                              : "hover:bg-gray-50"
                          }`}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="px-4 py-2 border rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    다음
                  </button>
                </div>
              )}
            </>
          )}
        </>
      )}
    </section>
  );
}
