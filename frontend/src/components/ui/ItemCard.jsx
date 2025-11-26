import { useState } from "react";
import MapModal from "../map/MapModal";
import HighlightText from "./HighlightText";

export default function ItemCard({ item, searchKeyword = "" }) {
  const {
    title,
    description,
    category,
    location,
    region,
    foundDate,
    status,
    color,
    storageLocation,
    dataSource,
    createdAt,
  } = item;

  // 지도 모달 상태
  const [isMapModalOpen, setIsMapModalOpen] = useState(false);

  // 날짜 포맷팅
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  };

  // 상태 한글 변환
  const getStatusText = (status) => {
    const statusMap = {
      FOUND: "습득",
      CLAIMED: "수령완료",
      EXPIRED: "만료",
      STORED: "보관중",
      RETURNED: "반환완료",
      DISPOSED: "폐기",
    };
    return statusMap[status] || status;
  };

  // 상태별 색상
  const getStatusColor = (status) => {
    const colorMap = {
      FOUND: "bg-green-100 text-green-800",
      CLAIMED: "bg-blue-100 text-blue-800",
      EXPIRED: "bg-red-100 text-red-800",
      STORED: "bg-yellow-100 text-yellow-800",
      RETURNED: "bg-purple-100 text-purple-800",
      DISPOSED: "bg-gray-100 text-gray-800",
    };
    return colorMap[status] || "bg-gray-100 text-gray-800";
  };

  // 출처 한글 변환
  const getDataSourceText = (source) => {
    const sourceMap = {
      USER: "사용자 등록",
      LOST112: "경찰청 분실물",
      SEOUL_LOST: "서울시 분실물",
    };
    return sourceMap[source] || source;
  };

  // 지도 보기 버튼 클릭 핸들러
  const handleMapClick = (e) => {
    e.stopPropagation(); // 카드 클릭 이벤트 전파 방지
    setIsMapModalOpen(true);
  };

  return (
    <>
      <div className="border rounded-lg p-4 hover:shadow-lg transition-shadow bg-white cursor-pointer">
        {/* 상태 배지 */}
        <div className="flex justify-between items-start mb-2">
          <div className="flex gap-1">
            <span
              className={`inline-block px-2 py-1 text-xs font-semibold rounded ${getStatusColor(
                status
              )}`}
            >
              {getStatusText(status)}
            </span>
            {region && (
              <span className="inline-block px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded">
                📍 {region}
              </span>
            )}
          </div>
          {category && (
            <span className="inline-block px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded">
              {category}
            </span>
          )}
        </div>

        {/* 제목 */}
        <h3 className="font-semibold text-lg mb-2 line-clamp-1">
          <HighlightText text={title} searchTerm={searchKeyword} />
        </h3>

        {/* 설명 */}
        {description && (
          <p className="text-sm text-gray-600 mb-3 line-clamp-2">
            <HighlightText text={description} searchTerm={searchKeyword} />
          </p>
        )}

        {/* 정보 */}
        <div className="space-y-1 text-sm text-gray-600">
          {location && (
            <div className="flex items-start">
              <span className="font-medium min-w-[60px]">습득장소:</span>
              <span className="line-clamp-1">{location}</span>
            </div>
          )}
          {storageLocation && (
            <div className="flex items-start">
              <span className="font-medium min-w-[60px]">보관장소:</span>
              <span className="line-clamp-1">{storageLocation}</span>
            </div>
          )}
          {foundDate && (
            <div className="flex items-start">
              <span className="font-medium min-w-[60px]">습득일:</span>
              <span>{formatDate(foundDate)}</span>
            </div>
          )}
          {color && (
            <div className="flex items-start">
              <span className="font-medium min-w-[60px]">색상:</span>
              <span>{color}</span>
            </div>
          )}
        </div>

        {/* 하단 정보 + 지도 보기 버튼 */}
        <div className="mt-3 pt-3 border-t flex justify-between items-center text-xs text-gray-500">
          <span>{getDataSourceText(dataSource)}</span>
          <div className="flex items-center gap-2">
            <span>{formatDate(createdAt)}</span>
            {(storageLocation || location) && (
              <button
                onClick={handleMapClick}
                className="ml-2 bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-xs font-semibold transition-colors flex items-center gap-1"
              >
                <span>🗺️</span>
                <span>지도 보기</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 지도 모달 */}
      <MapModal
        isOpen={isMapModalOpen}
        onClose={() => setIsMapModalOpen(false)}
        item={item}
      />
    </>
  );
}
