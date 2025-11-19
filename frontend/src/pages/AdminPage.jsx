import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { api } from "../lib/api";

export default function AdminPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState("dashboard");
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [collectingData, setCollectingData] = useState(false);
  const [collectionResult, setCollectionResult] = useState(null);

  // ✨ 통합 데이터 수집 관련 상태
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
      .toISOString()
      .split("T")[0],
    endDate: new Date().toISOString().split("T")[0],
  });
  const [collectLost112, setCollectLost112] = useState(true);
  const [collectSeoul, setCollectSeoul] = useState(true);
  const [autoCleanup, setAutoCleanup] = useState(true);

  // 시스템 통계 조회
  const fetchStats = async () => {
    try {
      setLoading(true);
      const { data } = await api.get("/api/admin/stats");
      setStats(data.data);
    } catch (err) {
      console.error("통계 조회 실패:", err);
      alert("통계 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 사용자 목록 조회
  const fetchUsers = async () => {
    try {
      setLoading(true);
      const { data } = await api.get("/api/admin/users");
      if (data.data && data.data.users) {
        setUsers(data.data.users);
      }
    } catch (err) {
      console.error("사용자 목록 조회 실패:", err);
      alert("사용자 목록 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 사용자 삭제
  const handleDeleteUser = async (userId, userEmail) => {
    if (!window.confirm(`정말로 ${userEmail} 사용자를 삭제하시겠습니까?`)) {
      return;
    }

    try {
      const { data } = await api.delete(`/api/admin/users/${userId}`);
      if (data.data && data.data.success) {
        alert("사용자가 삭제되었습니다.");
        fetchUsers(); // 목록 새로고침
      } else {
        alert(data.data?.message || "사용자 삭제에 실패했습니다.");
      }
    } catch (err) {
      console.error("사용자 삭제 실패:", err);
      alert(err?.response?.data?.message || "사용자 삭제에 실패했습니다.");
    }
  };

  // 데이터 수집 (기존)
  const handleCollectData = async () => {
    if (
      !window.confirm(
        "데이터 수집을 시작하시겠습니까? (시간이 걸릴 수 있습니다)"
      )
    ) {
      return;
    }

    try {
      setCollectingData(true);
      setCollectionResult(null);

      // LOST112 데이터 수집
      const { data } = await api.post("/api/admin/import/lost112", {
        maxPages: 5,
        rowsPerPage: 100,
      });

      setCollectionResult(data.data);
      alert("데이터 수집이 완료되었습니다!");
      fetchStats(); // 통계 새로고침
    } catch (err) {
      console.error("데이터 수집 실패:", err);
      alert(
        "데이터 수집에 실패했습니다: " +
          (err?.response?.data?.message || err.message)
      );
    } finally {
      setCollectingData(false);
    }
  };

  // ✨ 통합 데이터 수집 (권장)
  const handleIntegratedCollect = async () => {
    if (
      !window.confirm(
        "통합 데이터 수집을 시작하시겠습니까?\n\n선택 항목:\n" +
          (collectLost112 ? "✅ LOST112\n" : "") +
          (collectSeoul ? "✅ 서울교통공사\n" : "") +
          (autoCleanup ? "✅ 자동 정리\n" : "") +
          `\n기간: ${dateRange.startDate} ~ ${dateRange.endDate}`
      )
    ) {
      return;
    }

    try {
      setCollectingData(true);
      setCollectionResult(null);

      const { data } = await api.post("/api/admin/import-integrated", {
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        collectLost112,
        collectSeoul,
        autoCleanup,
        maxPages: 10,
        rowsPerPage: 100,
      });

      setCollectionResult(data.data);
      alert(data.data.message || "데이터 수집이 완료되었습니다!");
      fetchStats(); // 통계 새로고침
    } catch (err) {
      console.error("통합 데이터 수집 실패:", err);
      alert(
        "통합 데이터 수집에 실패했습니다: " +
          (err?.response?.data?.message || err.message)
      );
    } finally {
      setCollectingData(false);
    }
  };

  // 깨진 데이터 즉시 정리
  const handleCleanupNow = async () => {
    if (!window.confirm("깨진 데이터를 즉시 정리하시겠습니까?")) {
      return;
    }

    try {
      setLoading(true);
      const { data } = await api.post("/api/admin/cleanup-now");
      alert(data.data.message || "정리가 완료되었습니다!");
      fetchStats();
    } catch (err) {
      console.error("정리 실패:", err);
      alert(
        "정리에 실패했습니다: " + (err?.response?.data?.message || err.message)
      );
    } finally {
      setLoading(false);
    }
  };

  // 탭 변경 시 데이터 로드
  useEffect(() => {
    if (activeTab === "dashboard") {
      fetchStats();
    } else if (activeTab === "users") {
      fetchUsers();
    }
  }, [activeTab]);

  // 관리자 권한 체크
  if (!user || user.role !== "ADMIN") {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-red-500">관리자 권한이 필요합니다.</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 font-elice">
      <div className="max-w-7xl mx-auto px-4">
        {/* 헤더 */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <h1 className="text-3xl font-bold text-[#3D3D3D] mb-2">
            관리자 페이지
          </h1>
          <p className="text-gray-600">시스템 관리 및 데이터 수집</p>
        </div>

        {/* 탭 메뉴 */}
        <div className="bg-white rounded-lg shadow-sm mb-6">
          <div className="flex border-b">
            <button
              onClick={() => setActiveTab("dashboard")}
              className={`px-6 py-4 font-medium transition-colors ${
                activeTab === "dashboard"
                  ? "text-[#A2AADB] border-b-2 border-[#A2AADB]"
                  : "text-gray-600 hover:text-gray-800"
              }`}
            >
              대시보드
            </button>
            <button
              onClick={() => setActiveTab("users")}
              className={`px-6 py-4 font-medium transition-colors ${
                activeTab === "users"
                  ? "text-[#A2AADB] border-b-2 border-[#A2AADB]"
                  : "text-gray-600 hover:text-gray-800"
              }`}
            >
              사용자 관리
            </button>
            <button
              onClick={() => setActiveTab("data")}
              className={`px-6 py-4 font-medium transition-colors ${
                activeTab === "data"
                  ? "text-[#A2AADB] border-b-2 border-[#A2AADB]"
                  : "text-gray-600 hover:text-gray-800"
              }`}
            >
              데이터 수집
            </button>
          </div>
        </div>

        {/* 대시보드 탭 */}
        {activeTab === "dashboard" && (
          <div className="space-y-6">
            {loading ? (
              <div className="text-center py-12">
                <div className="text-gray-500">로딩 중...</div>
              </div>
            ) : stats ? (
              <>
                {/* 통계 카드 */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-semibold text-gray-700 mb-2">
                      전체 사용자
                    </h3>
                    <p className="text-3xl font-bold text-[#A2AADB]">
                      {stats.users?.total || 0}명
                    </p>
                    <p className="text-sm text-gray-600 mt-2">
                      관리자: {stats.users?.admin || 0}명 | 일반:{" "}
                      {stats.users?.normal || 0}명
                    </p>
                  </div>

                  <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-semibold text-gray-700 mb-2">
                      분실물 데이터
                    </h3>
                    <p className="text-3xl font-bold text-[#A2AADB]">
                      {stats.lostItems?.total?.toLocaleString() || 0}건
                    </p>
                    <p className="text-sm text-gray-600 mt-2">
                      지역정보:{" "}
                      {stats.lostItems?.withRegion?.toLocaleString() || 0}건
                    </p>
                  </div>

                  <div className="bg-white rounded-lg shadow-sm p-6">
                    <h3 className="text-lg font-semibold text-gray-700 mb-2">
                      지역 커버리지
                    </h3>
                    <p className="text-3xl font-bold text-[#A2AADB]">
                      {stats.lostItems?.regionCoverage || "0%"}
                    </p>
                    <p className="text-sm text-gray-600 mt-2">
                      데이터 품질 지표
                    </p>
                  </div>
                </div>

                {/* 시스템 정보 */}
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h3 className="text-xl font-semibold text-gray-800 mb-4">
                    시스템 정보
                  </h3>
                  <div className="space-y-2 text-sm text-gray-600">
                    <p>
                      마지막 업데이트:{" "}
                      {stats.timestamp
                        ? new Date(stats.timestamp).toLocaleString("ko-KR")
                        : "-"}
                    </p>
                    <p>
                      시스템 상태:{" "}
                      <span className="text-green-600 font-semibold">정상</span>
                    </p>
                  </div>
                </div>
              </>
            ) : (
              <div className="text-center py-12">
                <button
                  onClick={fetchStats}
                  className="px-6 py-3 bg-[#A2AADB] text-white rounded-lg hover:bg-[#8B94C7] transition-colors"
                >
                  통계 불러오기
                </button>
              </div>
            )}
          </div>
        )}

        {/* 사용자 관리 탭 */}
        {activeTab === "users" && (
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-semibold text-gray-800">
                사용자 목록
              </h3>
              <button
                onClick={fetchUsers}
                disabled={loading}
                className="px-4 py-2 bg-[#A2AADB] text-white rounded-md hover:bg-[#8B94C7] transition-colors disabled:opacity-50"
              >
                새로고침
              </button>
            </div>

            {loading ? (
              <div className="text-center py-12">
                <div className="text-gray-500">로딩 중...</div>
              </div>
            ) : users.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        ID
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        이메일
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        이름
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        권한
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        가입일
                      </th>
                      <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                        작업
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200">
                    {users.map((u) => (
                      <tr key={u.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-sm text-gray-600">
                          {u.id}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-800">
                          {u.email}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-800">
                          {u.name}
                        </td>
                        <td className="px-4 py-3 text-sm">
                          <span
                            className={`px-2 py-1 rounded text-xs font-semibold ${
                              u.role === "ADMIN"
                                ? "bg-purple-100 text-purple-800"
                                : "bg-blue-100 text-blue-800"
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600">
                          {u.createdAt
                            ? new Date(u.createdAt).toLocaleDateString("ko-KR")
                            : "-"}
                        </td>
                        <td className="px-4 py-3 text-sm">
                          {u.email !== user?.email && (
                            <button
                              onClick={() => handleDeleteUser(u.id, u.email)}
                              className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 transition-colors text-xs"
                            >
                              삭제
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-center py-12 text-gray-500">
                사용자가 없습니다.
              </div>
            )}
          </div>
        )}

        {/* 데이터 수집 탭 */}
        {activeTab === "data" && (
          <div className="space-y-6">
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h3 className="text-xl font-semibold text-gray-800 mb-4">
                데이터 수집
              </h3>
              <p className="text-gray-600 mb-6">
                LOST112 공공 데이터 API를 통해 최신 분실물 정보를 수집합니다.
              </p>

              {/* ✨ 통합 데이터 수집 UI (권장) */}
              <div className="bg-gradient-to-r from-purple-50 to-blue-50 rounded-lg p-6 mb-6">
                <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center">
                  ✨ 통합 데이터 수집 (권장)
                  <span className="ml-2 px-2 py-1 text-xs bg-green-500 text-white rounded">
                    NEW
                  </span>
                </h3>

                {/* 날짜 범위 선택 */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      시작 날짜
                    </label>
                    <input
                      type="date"
                      value={dateRange.startDate}
                      onChange={(e) =>
                        setDateRange({
                          ...dateRange,
                          startDate: e.target.value,
                        })
                      }
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#A2AADB] focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      종료 날짜
                    </label>
                    <input
                      type="date"
                      value={dateRange.endDate}
                      onChange={(e) =>
                        setDateRange({ ...dateRange, endDate: e.target.value })
                      }
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#A2AADB] focus:border-transparent"
                    />
                  </div>
                </div>

                {/* 수집 대상 선택 */}
                <div className="space-y-3 mb-4">
                  <label className="flex items-center space-x-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={collectLost112}
                      onChange={(e) => setCollectLost112(e.target.checked)}
                      className="w-5 h-5 text-[#A2AADB] rounded focus:ring-[#A2AADB]"
                    />
                    <span className="text-gray-700 font-medium">
                      경찰청 LOST112 데이터
                    </span>
                  </label>
                  <label className="flex items-center space-x-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={collectSeoul}
                      onChange={(e) => setCollectSeoul(e.target.checked)}
                      className="w-5 h-5 text-[#A2AADB] rounded focus:ring-[#A2AADB]"
                    />
                    <span className="text-gray-700 font-medium">
                      서울교통공사 데이터
                    </span>
                  </label>
                  <label className="flex items-center space-x-3 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={autoCleanup}
                      onChange={(e) => setAutoCleanup(e.target.checked)}
                      className="w-5 h-5 text-green-500 rounded focus:ring-green-500"
                    />
                    <span className="text-gray-700 font-medium">
                      수집 후 깨진 데이터 자동 정리
                    </span>
                  </label>
                </div>

                {/* 통합 수집 버튼 */}
                <div className="flex gap-3">
                  <button
                    onClick={handleIntegratedCollect}
                    disabled={
                      collectingData || (!collectLost112 && !collectSeoul)
                    }
                    className="flex-1 px-6 py-4 bg-gradient-to-r from-purple-500 to-blue-500 text-white rounded-lg hover:from-purple-600 hover:to-blue-600 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold text-lg shadow-md"
                  >
                    {collectingData ? "🔄 수집 중..." : "✨ 통합 수집 시작"}
                  </button>
                  <button
                    onClick={handleCleanupNow}
                    disabled={loading}
                    className="px-6 py-4 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-semibold text-lg shadow-md"
                  >
                    🧹 즉시 정리
                  </button>
                </div>
              </div>

              {/* 기존 데이터 수집 버튼 (레거시) */}
              <div className="bg-gray-50 rounded-lg p-4 mb-6">
                <p className="text-sm text-gray-600 mb-3">
                  또는 기존 방식으로 수집:
                </p>
                <button
                  onClick={handleCollectData}
                  disabled={collectingData}
                  className="w-full md:w-auto px-8 py-4 bg-gray-400 text-white rounded-lg hover:bg-gray-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-semibold text-lg"
                >
                  {collectingData ? "데이터 수집 중..." : "기존 방식으로 수집"}
                </button>
              </div>

              {collectingData && (
                <div className="mt-6 p-4 bg-blue-50 rounded-lg">
                  <p className="text-blue-700">
                    데이터를 수집하고 있습니다. 잠시만 기다려주세요...
                  </p>
                </div>
              )}
            </div>

            {/* 수집 결과 */}
            {collectionResult && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h3 className="text-xl font-semibold text-gray-800 mb-4">
                  수집 결과
                </h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
                    <span className="text-gray-700">수집 성공 여부</span>
                    <span
                      className={`font-semibold ${
                        collectionResult.success
                          ? "text-green-600"
                          : "text-red-600"
                      }`}
                    >
                      {collectionResult.success ? "성공" : "실패"}
                    </span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
                    <span className="text-gray-700">총 수집 건수</span>
                    <span className="font-semibold text-[#A2AADB]">
                      {collectionResult.totalFetched || 0}건
                    </span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
                    <span className="text-gray-700">신규 저장 건수</span>
                    <span className="font-semibold text-green-600">
                      {collectionResult.newlyCreated || 0}건
                    </span>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
                    <span className="text-gray-700">중복 제외 건수</span>
                    <span className="font-semibold text-gray-600">
                      {collectionResult.duplicatesSkipped || 0}건
                    </span>
                  </div>
                  {collectionResult.message && (
                    <div className="p-3 bg-blue-50 rounded">
                      <p className="text-sm text-blue-700">
                        {collectionResult.message}
                      </p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
