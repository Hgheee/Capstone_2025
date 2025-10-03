import { useEffect, useState } from "react";
import axios from "axios";

// 최근 등록된 분실물 컴포넌트
function RecentLostItems() {
  const [lostItems, setLostItems] = useState([]);

  useEffect(() => {
    axios.get("http://localhost:8080/api/lost-items/recent")
      .then((res) => setLostItems(res.data))
      .catch((err) => console.error("데이터 불러오기 실패:", err));
  }, []);

  return (
    <div className="p-4">
      <h2 className="text-xl font-bold mb-3"> 최근 등록된 분실물 리스트</h2>
      {lostItems.length === 0 ? (
        <p>아직 등록된 분실물이 없습니다.</p>
      ) : (
        <ul className="space-y-2">
          {lostItems.map((item) => (
            <li key={item.id} className="p-3 border rounded-lg shadow-sm hover:bg-gray-50">
              <h3 className="font-semibold">{item.title}</h3>
              <p className="text-sm text-gray-600">{item.description}</p>
              <span className="text-xs text-gray-400">
                등록일: {new Date(item.createdAt).toLocaleDateString()}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

// 기존 Home 컴포넌트
export default function Home() {
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState("");
  const [place, setPlace] = useState("");

  const loadItems = () => {
    axios
      .get("http://localhost:8080/api/lost-items")
      .then((res) => setItems(res.data))
      .catch((err) => console.error("불러오기 오류:", err));
  };

  useEffect(() => {
    loadItems();
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    axios
      .post("http://localhost:8080/api/lost-items", { title, place })
      .then(() => {
        setTitle("");
        setPlace("");
        loadItems(); // 등록 후 목록 갱신
      })
      .catch((err) => console.error("등록 오류:", err));
  };

  return (
    <div style={{ padding: "20px" }}>
      <h1>Lost & Found</h1>

      <form onSubmit={handleSubmit} style={{ marginBottom: "20px" }}>
        <input
          placeholder="분실물 이름"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <input
          placeholder="장소"
          value={place}
          onChange={(e) => setPlace(e.target.value)}
        />
        <button type="submit">등록</button>
      </form>

      <ul>
        {items.map((item) => (
          <li key={item.id}>
            <b>{item.title}</b> - {item.place} ({item.status})
          </li>
        ))}
      </ul>

      {/* 여기서 최근 등록된 분실물 컴포넌트 추가 */}
      <RecentLostItems />
    </div>
  );
}
