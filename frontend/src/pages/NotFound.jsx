import { Link } from "react-router-dom";
export default function NotFound() {
  return (
    <div className="text-center space-y-4">
      <h2 className="text-2xl font-semibold">페이지를 찾을 수 없어요 </h2>
      <Link to="/" className="underline">
        홈으로
      </Link>
    </div>
  );
}
