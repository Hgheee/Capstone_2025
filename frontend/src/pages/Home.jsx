import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import SearchIcon from "../components/icons/SearchIcon.jsx";
import ItemCard from "../components/ui/ItemCard.jsx";
import { lostItemApi } from "../lib/api";

export default function Home() {
  const [query, setQuery] = useState("");
  const [recentItems, setRecentItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // 최근 분실물 불러오기
  const fetchRecentItems = async () => {
    setLoading(true);
    try {
      const response = await lostItemApi.getRecent();
      // 백엔드 ApiResponse 구조: { success, data, error }
      if (response.data.success) {
        setRecentItems(Array.isArray(response.data.data) ? response.data.data : []);
      } else {
        console.error("최근 분실물 로딩 실패:", response.data.error);
        setRecentItems([]);
      }
    } catch (err) {
      console.error("최근 분실물 로딩 오류:", err);
      setRecentItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecentItems();
  }, []);

  // 검색 버튼 클릭 시 Search 페이지로 이동
  const onSearch = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
    } else {
      navigate("/search");
    }
  };

  // Enter 키 처리
  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      onSearch(e);
    }
  };

  return (
    <div className="w-full bg-white">
      {/* Hero 영역 */}
      <section className="w-full h-[300px] bg-gradient-to-r from-[#A2AADB] to-[#6E6E6E] flex flex-col justify-center items-center text-white">
        <h1 className="text-4xl md:text-5xl font-bold mb-4 font-elice">
          분실물 통합 조회 서비스
        </h1>
        <p className="text-lg md:text-xl">
          경찰청, 지하철, 대학교 분실물 데이터를 한 곳에서!
        </p>
      </section>

      {/* 검색 영역 */}
      <section className="max-w-[1100px] mx-auto py-12 px-4">
        <form onSubmit={onSearch} className="flex items-center border border-gray-400 rounded-full overflow-hidden shadow-sm">
          <input
            type="text"
            placeholder="분실물을 검색하세요..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyPress={handleKeyPress}
            className="flex-grow px-6 py-4 text-lg focus:outline-none"
          />
          <button
            type="submit"
            className="w-[60px] h-[60px] flex items-center justify-center bg-[#A2AADB] text-white hover:bg-[#8890c8] transition-colors"
          >
            <SearchIcon className="w-7 h-7" />
          </button>
        </form>
      </section>

      {/* 최근 등록된 분실물 */}
      <section className="max-w-[1100px] mx-auto px-4 pb-12">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold">최근 등록된 분실물</h2>
          <button
            onClick={() => navigate("/search")}
            className="text-[#A2AADB] hover:text-[#8890c8] font-medium"
          >
            전체보기 →
          </button>
        </div>

        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-[#A2AADB]"></div>
            <p className="mt-4 text-gray-600">로딩 중...</p>
          </div>
        ) : recentItems.length === 0 ? (
          <div className="text-center py-12 bg-gray-50 rounded-lg">
            <p className="text-gray-500 text-lg">등록된 분실물이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {recentItems.slice(0, 6).map((item) => (
              <ItemCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </section>

      {/* 안내 섹션 */}
      <section className="bg-gray-50 py-16">
        <div className="max-w-[1100px] mx-auto px-4">
          <h2 className="text-2xl font-bold text-center mb-12">
            분실물 찾기가 더 쉬워졌습니다
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="text-center">
              <div className="w-16 h-16 bg-[#A2AADB] rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white text-2xl">🔍</span>
              </div>
              <h3 className="font-bold text-lg mb-2">통합 검색</h3>
              <p className="text-gray-600">
                여러 기관의 분실물 정보를 한 번에 검색할 수 있습니다.
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-[#A2AADB] rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white text-2xl">📍</span>
              </div>
              <h3 className="font-bold text-lg mb-2">지역별 검색</h3>
              <p className="text-gray-600">
                지역과 카테고리를 선택하여 원하는 분실물을 빠르게 찾으세요.
              </p>
            </div>
            <div className="text-center">
              <div className="w-16 h-16 bg-[#A2AADB] rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white text-2xl">⚡</span>
              </div>
              <h3 className="font-bold text-lg mb-2">실시간 업데이트</h3>
              <p className="text-gray-600">
                최신 분실물 정보를 실시간으로 확인할 수 있습니다.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
