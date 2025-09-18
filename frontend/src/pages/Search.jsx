import { useMemo, useState } from "react";
import data from "../mock/items.json";
import ItemCard from "../components/ItemCard.jsx";

export default function Search() {
  const [q, setQ] = useState("");

  const results = useMemo(() => {
    const keyword = q.trim().toLowerCase();
    if (!keyword) return data;
    return data.filter((d) =>
      [d.title, d.place, d.source].some((v) =>
        v.toLowerCase().includes(keyword)
      )
    );
  }, [q]);

  return (
    <section className="space-y-4">
      <h2 className="text-xl font-semibold">검색</h2>
      <form className="flex gap-2" onSubmit={(e) => e.preventDefault()}>
        <input
          type="text"
          name="q"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="예: 지갑, 우산, 에어팟"
          className="input input-bordered border rounded-md px-3 py-2 flex-1"
        />
        <button type="submit" className="px-4 py-2 rounded-md border">
          검색
        </button>
      </form>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {results.map((item) => (
          <ItemCard key={item.id} {...item} />
        ))}
      </div>
    </section>
  );
}
