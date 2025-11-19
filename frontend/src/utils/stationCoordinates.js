/**
 * 서울 지하철역 좌표 데이터베이스
 * 네이버 Geocoding API 실패 시 대체 사용
 */

export const STATION_COORDINATES = {
  // 1호선
  서울역: { lat: 37.5547125, lng: 126.9707878, line: "1호선" },
  시청역: { lat: 37.5666102, lng: 126.9783881, line: "1호선" },
  종각역: { lat: 37.5700796, lng: 126.9828482, line: "1호선" },
  종로3가역: { lat: 37.5714066, lng: 126.9925682, line: "1호선" },
  종로5가역: { lat: 37.5720854, lng: 127.0031567, line: "1호선" },
  동대문역: { lat: 37.5711, lng: 127.0094 },
  청량리역: { lat: 37.5800934, lng: 127.0479019 },

  // 2호선
  강남역: { lat: 37.4979, lng: 127.0276, line: "2호선" },
  역삼역: { lat: 37.5003761, lng: 127.0363183, line: "2호선" },
  선릉역: { lat: 37.5047581, lng: 127.0493451, line: "2호선" },
  삼성역: { lat: 37.5085, lng: 127.0631, line: "2호선" },
  종합운동장역: { lat: 37.5107, lng: 127.073, line: "2호선" },
  잠실역: { lat: 37.5133145, lng: 127.1001673, line: "2호선" },
  신림역: { lat: 37.4843098, lng: 126.9295857, line: "2호선" },
  봉천역: { lat: 37.4823166, lng: 126.9425645, line: "2호선" },
  신도림역: { lat: 37.5089, lng: 126.891, line: "2호선" },
  대림역: { lat: 37.4933, lng: 126.8954, line: "2호선" },
  구로디지털단지역: { lat: 37.485, lng: 126.9012, line: "2호선" },
  홍대입구역: { lat: 37.5579, lng: 126.924, line: "2호선" },
  신촌역: { lat: 37.5559, lng: 126.9364, line: "2호선" },
  이대역: { lat: 37.5567, lng: 126.9457, line: "2호선" },
  아현역: { lat: 37.5577, lng: 126.9563, line: "2호선" },
  충정로역: { lat: 37.5603, lng: 126.9641, line: "2호선" },

  // 3호선
  양재역: { lat: 37.484, lng: 127.0342, line: "3호선" },
  매봉역: { lat: 37.4802, lng: 127.0445, line: "3호선" },
  도곡역: { lat: 37.4929, lng: 127.054, line: "3호선" },
  대치역: { lat: 37.4942, lng: 127.0631, line: "3호선" },
  학여울역: { lat: 37.4966, lng: 127.0734, line: "3호선" },
  고속터미널역: { lat: 37.504, lng: 127.005, line: "3호선" },
  교대역: { lat: 37.4933, lng: 127.0142, line: "3호선" },
  남부터미널역: { lat: 37.4769, lng: 126.9958, line: "3호선" },

  // 4호선
  사당역: { lat: 37.4767, lng: 126.9813, line: "4호선" },
  총신대입구역: { lat: 37.4862, lng: 126.9824, line: "4호선" },
  동작역: { lat: 37.4998, lng: 126.9819, line: "4호선" },
  이촌역: { lat: 37.5221, lng: 126.975, line: "4호선" },
  명동역: { lat: 37.5606, lng: 126.986, line: "4호선" },
  회현역: { lat: 37.5591, lng: 126.9782, line: "4호선" },
  서울역: { lat: 37.5547125, lng: 126.9707878, line: "4호선" },
  숙대입구역: { lat: 37.5451, lng: 126.9648, line: "4호선" },
  삼각지역: { lat: 37.5345, lng: 126.9727, line: "4호선" },
  신용산역: { lat: 37.5291, lng: 126.9645, line: "4호선" },

  // 5호선
  김포공항역: { lat: 37.5629, lng: 126.8013, line: "5호선" },
  송정역: { lat: 37.5628, lng: 126.814, line: "5호선" },
  마곡역: { lat: 37.5609, lng: 126.8255, line: "5호선" },
  발산역: { lat: 37.5588, lng: 126.8374, line: "5호선" },
  여의도역: { lat: 37.5217, lng: 126.9244, line: "5호선" },
  여의나루역: { lat: 37.527, lng: 126.9343, line: "5호선" },
  마포역: { lat: 37.5396, lng: 126.9453, line: "5호선" },
  공덕역: { lat: 37.5443, lng: 126.9515, line: "5호선" },
  왕십리역: { lat: 37.5617, lng: 127.0385, line: "5호선" },
  광나루역: { lat: 37.5451, lng: 127.111, line: "5호선" },
  천호역: { lat: 37.5389, lng: 127.1237, line: "5호선" },

  // 6호선
  응암역: { lat: 37.6019, lng: 126.9175, line: "6호선" },
  역촌역: { lat: 37.6014, lng: 126.9273, line: "6호선" },
  불광역: { lat: 37.6101, lng: 126.929, line: "6호선" },
  연신내역: { lat: 37.6196, lng: 126.9209, line: "6호선" },
  독바위역: { lat: 37.6254, lng: 126.9268, line: "6호선" },
  합정역: { lat: 37.5497, lng: 126.9136, line: "6호선" },
  상수역: { lat: 37.5478, lng: 126.9227, line: "6호선" },
  광흥창역: { lat: 37.551, lng: 126.9313, line: "6호선" },
  대흥역: { lat: 37.5545, lng: 126.9384, line: "6호선" },
  공덕역: { lat: 37.5443, lng: 126.9515, line: "6호선" },
  효창공원앞역: { lat: 37.5396, lng: 126.9613, line: "6호선" },
  삼각지역: { lat: 37.5345, lng: 126.9727, line: "6호선" },

  // 7호선
  장암역: { lat: 37.6997, lng: 127.0262, line: "7호선" },
  도봉산역: { lat: 37.689, lng: 127.0468, line: "7호선" },
  수락산역: { lat: 37.6773, lng: 127.0764, line: "7호선" },
  노원역: { lat: 37.6541, lng: 127.0616, line: "7호선" },
  태릉입구역: { lat: 37.6174, lng: 127.0753, line: "7호선" },
  공릉역: { lat: 37.6258, lng: 127.0728, line: "7호선" },
  하계역: { lat: 37.6362, lng: 127.0679, line: "7호선" },
  중계역: { lat: 37.6498, lng: 127.0725, line: "7호선" },
  건대입구역: { lat: 37.5408, lng: 127.0701, line: "7호선" },
  강남구청역: { lat: 37.5176, lng: 127.0411, line: "7호선" },
  논현역: { lat: 37.5108, lng: 127.0224, line: "7호선" },
  반포역: { lat: 37.5009, lng: 127.0117, line: "7호선" },
  고속터미널역: { lat: 37.504, lng: 127.005, line: "7호선" },
  내방역: { lat: 37.4987, lng: 126.9946, line: "7호선" },
  이수역: { lat: 37.4857, lng: 126.9811, line: "7호선" },
  남성역: { lat: 37.4774, lng: 126.9818, line: "7호선" },

  // 8호선
  암사역: { lat: 37.5516, lng: 127.1287, line: "8호선" },
  천호역: { lat: 37.5389, lng: 127.1237, line: "8호선" },
  강동구청역: { lat: 37.5301, lng: 127.1236, line: "8호선" },
  몽촌토성역: { lat: 37.5143, lng: 127.0885, line: "8호선" },
  잠실역: { lat: 37.5133145, lng: 127.1001673, line: "8호선" },
  석촌역: { lat: 37.5054, lng: 127.1056, line: "8호선" },
  송파역: { lat: 37.5037, lng: 127.1116, line: "8호선" },
  가락시장역: { lat: 37.492, lng: 127.1187, line: "8호선" },
  문정역: { lat: 37.4854, lng: 127.1222, line: "8호선" },
  장지역: { lat: 37.4786, lng: 127.1262, line: "8호선" },

  // 9호선
  개화역: { lat: 37.5783, lng: 126.799, line: "9호선" },
  김포공항역: { lat: 37.5629, lng: 126.8013, line: "9호선" },
  공항시장역: { lat: 37.5631, lng: 126.8128, line: "9호선" },
  신방화역: { lat: 37.5508, lng: 126.8134, line: "9호선" },
  마곡나루역: { lat: 37.5659, lng: 126.8298, line: "9호선" },
  양천향교역: { lat: 37.5509, lng: 126.8662, line: "9호선" },
  가양역: { lat: 37.5613, lng: 126.8552, line: "9호선" },
  증미역: { lat: 37.5664, lng: 126.8647, line: "9호선" },
  등촌역: { lat: 37.5508, lng: 126.8655, line: "9호선" },
  염창역: { lat: 37.5466, lng: 126.8744, line: "9호선" },
  신목동역: { lat: 37.5396, lng: 126.8794, line: "9호선" },
  선유도역: { lat: 37.5351, lng: 126.893, line: "9호선" },
  당산역: { lat: 37.5345, lng: 126.9023, line: "9호선" },
  국회의사당역: { lat: 37.5296, lng: 126.9165, line: "9호선" },
  여의도역: { lat: 37.5217, lng: 126.9244, line: "9호선" },
  샛강역: { lat: 37.5176, lng: 126.9123, line: "9호선" },
  노량진역: { lat: 37.5141, lng: 126.9426, line: "9호선" },
  노들역: { lat: 37.5087, lng: 126.9338, line: "9호선" },
  흑석역: { lat: 37.5084, lng: 126.961, line: "9호선" },
  동작역: { lat: 37.4998, lng: 126.9819, line: "9호선" },
  구반포역: { lat: 37.5067, lng: 126.9967, line: "9호선" },
  신반포역: { lat: 37.5042, lng: 127.0042, line: "9호선" },
  고속터미널역: { lat: 37.504, lng: 127.005, line: "9호선" },
  사평역: { lat: 37.4922, lng: 127.0078, line: "9호선" },
  신논현역: { lat: 37.5044, lng: 127.025, line: "9호선" },
  언주역: { lat: 37.5118, lng: 127.0347, line: "9호선" },
  선정릉역: { lat: 37.5048, lng: 127.0496, line: "9호선" },
  삼성중앙역: { lat: 37.5088, lng: 127.0598, line: "9호선" },
  봉은사역: { lat: 37.5145, lng: 127.0622, line: "9호선" },
  종합운동장역: { lat: 37.5107, lng: 127.073, line: "9호선" },

  // 공항철도
  인천공항1터미널역: { lat: 37.4607, lng: 126.4407, line: "공항철도" },
  인천공항2터미널역: { lat: 37.4937, lng: 126.4244, line: "공항철도" },

  // 주요 환승역
  용산역: { lat: 37.5296, lng: 126.9645, line: "중앙선" },
  수원역: { lat: 37.2659, lng: 127.0011, line: "1호선" },
  인천역: { lat: 37.4765, lng: 126.616, line: "1호선" },
  부평역: { lat: 37.4905, lng: 126.7227, line: "1호선" },

  // 주요 도시 역 (KTX/일반열차)
  부산역: { lat: 35.1156, lng: 129.0422, line: "KTX" },
  대구역: { lat: 35.877, lng: 128.6284, line: "KTX" },
  대전역: { lat: 36.3321, lng: 127.4342, line: "KTX" },
  광주송정역: { lat: 35.1372, lng: 126.7914, line: "KTX" },
  울산역: { lat: 35.5389, lng: 129.3594, line: "KTX" },
  창원역: { lat: 35.2236, lng: 128.6819, line: "KTX" },
  전주역: { lat: 35.8219, lng: 127.1481, line: "KTX" },
  여수엑스포역: { lat: 34.7614, lng: 127.6622, line: "KTX" },
  목포역: { lat: 34.7947, lng: 126.4311, line: "KTX" },
  강릉역: { lat: 37.7519, lng: 128.8961, line: "KTX" },
  포항역: { lat: 36.0194, lng: 129.3436, line: "KTX" },
};

/**
 * 역 이름에서 좌표 찾기
 * @param {string} stationName - 역 이름 (예: "강남역", "강남")
 * @returns {{lat: number, lng: number, line: string} | null}
 */
export const getStationCoordinates = (stationName) => {
  if (!stationName) return null;

  // "역" 제거
  let cleanName = stationName.replace(/역$/, "").trim();

  // 직접 매칭
  if (STATION_COORDINATES[cleanName + "역"]) {
    return STATION_COORDINATES[cleanName + "역"];
  }

  // "역" 없이 매칭
  if (STATION_COORDINATES[cleanName]) {
    return STATION_COORDINATES[cleanName];
  }

  // 부분 매칭 시도
  for (const [key, value] of Object.entries(STATION_COORDINATES)) {
    if (key.includes(cleanName) || cleanName.includes(key.replace("역", ""))) {
      return value;
    }
  }

  return null;
};

/**
 * 주소 문자열에서 역 이름 추출
 * @param {string} address - 주소 문자열
 * @returns {string | null}
 */
export const extractStationName = (address) => {
  if (!address) return null;

  // "역" 포함된 단어 찾기
  const stationMatch = address.match(/([가-힣]+역)/);
  if (stationMatch) {
    return stationMatch[1];
  }

  // 알려진 역 이름 찾기
  for (const stationName of Object.keys(STATION_COORDINATES)) {
    const cleanName = stationName.replace("역", "");
    if (address.includes(cleanName)) {
      return stationName;
    }
  }

  return null;
};
