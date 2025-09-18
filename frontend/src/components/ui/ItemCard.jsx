export default function ItemCard({ title, date, place, source }) {
  return (
    <div className="border rounded-lg p-4">
      <h3 className="font-medium">{title}</h3>
      <p className="text-sm text-gray-600">
        {date} · {place}
      </p>
      <p className="text-xs text-gray-500 mt-2">출처: {source}</p>
    </div>
  );
}
