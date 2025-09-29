import { useState, useEffect } from "react";
import SearchIcon from "../components/icons/SearchIcon.jsx";
import api from "../api/axios";

export default function Home() {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchItems = async (q = "") => {
    setLoading(true);
    try {
      // 예: GET /lost-items?query=...
      const { data } = await api.get("/lost-items", {
        params: q ? { query: q } : undefined,
      });
      // 백엔드 응답 형태에 맞게 조정
      setItems(data.items || data || []);
    } catch (err) {
      console.error(err);
      alert("목록을 불러오는 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchItems();
  }, []);

  const onSearch = () => fetchItems(query);

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
        <div className="flex items-center border border-gray-400 rounded-full overflow-hidden shadow-sm">
          <input
            type="text"
            placeholder="분실물을 검색하세요..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="flex-grow px-6 py-4 text-lg focus:outline-none"
          />
          <button
            onClick={onSearch}
            className="w-[60px] h-[60px] flex items-center justify-center bg-[#A2AADB] text-white"
          >
            <SearchIcon className="w-7 h-7" />
          </button>
        </div>
      </section>

      {/* 분실물 카드 리스트 예시 */}
      <section className="max-w-[1100px] mx-auto grid grid-cols-1 md:grid-cols-3 gap-6 px-4 pb-12">
        {[1, 2, 3, 4, 5, 6].map((item) => (
          <div
            key={item}
            className="border rounded-lg shadow-sm p-6 bg-white hover:shadow-md transition"
          >
            <h2 className="font-bold text-lg mb-2">분실물 {item}</h2>
            <p className="text-sm text-gray-600">
              여기에 분실물 간단 설명 또는 데이터 미리보기를 표시합니다.
            </p>
          </div>
        ))}
      </section>
    </div>
  );
}
