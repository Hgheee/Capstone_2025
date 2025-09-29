import { useState } from "react";
import SearchIcon from "../components/icons/SearchIcon.jsx";

export default function Home() {
  const [items, setItems] = useState([]);

  const onSearch = () => {
    // TODO: 백엔드 /api/items 연결
    console.log("검색 실행");
  };

  return (
    <div className="space-y-6">
      {/* 상단 영역 */}
      <section className="border border-gray-500 p-6 rounded-lg">
        <div className="flex items-center border border-gray-400 rounded-[25px] p-2 bg-white">
          <input
            type="text"
            placeholder="검색어를 입력하세요"
            className="flex-grow h-[62px] rounded-[25px] bg-gray-200 px-4 outline-none"
          />
          <button
            onClick={onSearch}
            className="ml-2 w-[63px] h-[63px] flex items-center justify-center rounded-full bg-gray-500 text-white"
          >
            <SearchIcon className="w-7 h-7" />
          </button>
        </div>

        {/* Figma Rectangle 15 → 첫 번째 카드 영역 */}
        <div className="mt-6 h-[245px] bg-gray-300 rounded-lg flex items-center justify-center">
          <span className="text-gray-600">[최근 분실물 카드 리스트 자리]</span>
        </div>
      </section>

      {/* Figma Rectangle 16 → 두 번째 카드 영역 */}
      <section className="h-[231px] bg-gray-300 rounded-lg flex items-center justify-center">
        <span className="text-gray-600">[추가 콘텐츠 영역]</span>
      </section>
    </div>
  );
}
