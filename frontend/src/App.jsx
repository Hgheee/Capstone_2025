import { useEffect, useState } from "react";
import axios from "axios";

function App() {
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState("");
  const [place, setPlace] = useState("");

  const loadItems = () => {
    axios
      .get("http://localhost:8080/api/lost-items")
      .then((res) => setItems(res.data));
  };

  useEffect(() => {
    loadItems();
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    axios
      .post("http://localhost:8080/api/lost-items", {
        title,
        place,
      })
      .then(() => {
        setTitle("");
        setPlace("");
        loadItems(); // 등록 후 목록 갱신
      });
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
    </div>
  );
}

export default App;
