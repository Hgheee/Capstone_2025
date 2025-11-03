import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { lostItemApi } from "../lib/api";
import ItemCard from "../components/ui/ItemCard.jsx";

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

// 지역 옵션 정의 (서울 주요 구 및 기타 지역)
const REGIONS = [
  "전체",
  "강남구", "강동구", "강북구", "강서구",
  "관악구", "광진구", "구로구", "금천구",
  "노원구", "도봉구", "동대문구", "동작구",
  "마포구", "서대문구", "서초구", "성동구",
  "성북구", "송파구", "양천구", "영등포구",
  "용산구", "은평구", "종로구", "중구", "중랑구"
];

export default function Search() {
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("전체");
  const [selectedRegion, setSelectedRegion] = useState("전체");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // URL 쿼리 파라미터에서 초기 검색어 가져오기
  useEffect(() => {
    const queryParam = searchParams.get("q");
    if (queryParam) {
      setKeyword(queryParam);
    }
  }, [searchParams]);

  // 검색 실행 함수
  const performSearch = async (page = 0) => {
    setLoading(true);
    setError(null);
    
    try {
      let response;
      
      // 고급 검색 (키워드, 카테고리, 지역 조합)
      const searchParams = {
        page,
        size: 12,
        sort: "createdAt,desc"
      };

      // 키워드가 있으면 추가
      if (keyword.trim()) {
        searchParams.keyword = keyword.trim();
      }

      // 카테고리가 선택되었으면 추가
      if (selectedCategory !== "전체") {
        searchParams.category = selectedCategory;
      }

      // 지역 검색은 별도 처리가 필요하므로 조건 분기
      if (selectedRegion !== "전체" && !keyword && selectedCategory === "전체") {
        // 지역만 선택된 경우
        response = await lostItemApi.searchByRegion(selectedRegion, searchParams);
      } else if (selectedRegion !== "전체") {
        // 지역과 다른 조건이 함께 있는 경우 - 전체 텍스트 검색 사용
        const searchText = [keyword, selectedRegion].filter(Boolean).join(" ");
        response = await lostItemApi.searchFullText(searchText, searchParams);
      } else if (Object.keys(searchParams).length > 3) {
        // 키워드나 카테고리가 있는 경우 고급 검색 사용
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
    performSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  // 검색 버튼 클릭 또는 폼 제출
  const handleSubmit = (e) => {
    e.preventDefault();
    performSearch(0);
  };

  // 페이지 변경
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      performSearch(newPage);
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
          <div className="flex gap-2">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="예: 지갑, 우산, 에어팟"
              className="flex-1 border rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
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
                onClick={() => {
                  setSelectedCategory(category);
                  setCurrentPage(0);
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

        {/* 지역 선택 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            지역
          </label>
          <select
            value={selectedRegion}
            onChange={(e) => {
              setSelectedRegion(e.target.value);
              setCurrentPage(0);
            }}
            className="w-full border rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {REGIONS.map((region) => (
              <option key={region} value={region}>
                {region}
              </option>
            ))}
          </select>
        </div>
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
                  <ItemCard key={item.id} item={item} />
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
