export default function Report() {
  return (
    <section className="space-y-4 max-w-xl">
      <h2 className="text-xl font-semibold">분실물 등록</h2>
      <form className="space-y-3">
        <input
          className="w-full border rounded-md px-3 py-2"
          placeholder="분실물 제목"
        />
        <textarea
          className="w-full border rounded-md px-3 py-2"
          rows="4"
          placeholder="상세 설명"
        />
        <div className="flex gap-2">
          <input
            className="flex-1 border rounded-md px-3 py-2"
            placeholder="분실 위치(선택)"
          />
          <input
            className="w-40 border rounded-md px-3 py-2"
            placeholder="분실 날짜"
          />
        </div>
        <button className="px-4 py-2 rounded-md border">등록</button>
      </form>
    </section>
  );
}
