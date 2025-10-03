// 재사용 카드 컴포넌트
export default function LostItemCard({
  title,
  category,
  date,          // ISO 문자열 또는 Date
  source,        // 기관명
  href,          // 상세 페이지 링크(선택)
  onClick,       // 클릭 핸들러(선택)
}) {
  const formatted = date
    ? new Date(date).toLocaleDateString()
    : "-";

  const Wrapper = ({ children }) =>
    href ? (
      <a href={href} className="block group" onClick={onClick}>{children}</a>
    ) : (
      <div className="group" onClick={onClick}>{children}</div>
    );

  return (
    <Wrapper>
      <article className="rounded-2xl border bg-white/80 shadow-sm p-4 hover:shadow-md transition-all">
        {/* 카테고리 배지 */}
        <div className="mb-2 inline-flex items-center gap-2">
          <span className="px-2.5 py-0.5 text-xs rounded-full bg-gray-100 text-gray-700">
            {category || "미분류"}
          </span>
        </div>

        {/* 제목 */}
        <h3 className="text-base font-semibold leading-snug line-clamp-2 group-hover:underline">
          {title || "제목 없음"}
        </h3>

        {/* 메타 영역 */}
        <div className="mt-3 flex items-center justify-between text-sm text-gray-600">
          <span className="inline-flex items-center gap-1">
            <svg width="16" height="16" viewBox="0 0 24 24" className="opacity-70">
              <path fill="currentColor" d="M7 10h10v2H7zm0 4h7v2H7zM19 4h-1V2h-2v2H8V2H6v2H5c-1.1 0-2 .9-2 2v12
              c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2m0 14H5V9h14z"/>
            </svg>
            {formatted}
          </span>
          <span className="truncate max-w-[55%] text-right" title={source}>
            {source || "출처 미상"}
          </span>
        </div>
      </article>
    </Wrapper>
  );
}
