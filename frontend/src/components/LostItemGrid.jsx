import LostItemCard from "./LostItemCard";

export default function LostItemGrid({ items = [] }) {
  if (!items.length) {
    return (
      <div className="text-center text-gray-500 py-10">
        아직 등록된 항목이 없습니다.
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((it) => (
        <LostItemCard
          key={it.id}
          title={it.title}
          category={it.category || it.type || "분실물"}
          date={it.createdAt || it.date}
          source={it.sourceOrg || it.source || it.place}  // 백엔드 필드에 맞춰 매핑
          href={`/items/${it.id}`}
        />
      ))}
    </div>
  );
}
