package com.lostfound.capstonebackend.common.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 전국 지역 정보 추출 및 근처 지역 검색을 위한 유틸리티 클래스입니다.
 * 광역시/도 -> 시/군/구 계층 구조로 동작합니다.
 */
public class RegionUtil {

    /**
     * 광역시/도 목록
     */
    public static final List<String> PROVINCES = Arrays.asList(
            "서울특별시", "서울시", "서울",
            "부산광역시", "부산시", "부산",
            "대구광역시", "대구시", "대구",
            "인천광역시", "인천시", "인천",
            "광주광역시", "광주시", "광주",
            "대전광역시", "대전시", "대전",
            "울산광역시", "울산시", "울산",
            "세종특별자치시", "세종시", "세종",
            "경기도", "경기",
            "강원특별자치도", "강원도", "강원",
            "충청북도", "충북",
            "충청남도", "충남",
            "전북특별자치도", "전라북도", "전북",
            "전라남도", "전남",
            "경상북도", "경북",
            "경상남도", "경남",
            "제주특별자치도", "제주도", "제주"
    );

    /**
     * 각 광역시/도별 시/군/구 목록
     */
    private static final Map<String, List<String>> REGION_HIERARCHY = new HashMap<>() {{
        // 서울특별시
        put("서울", Arrays.asList(
                "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
                "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
                "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
        ));

        // 부산광역시
        put("부산", Arrays.asList(
                "강서구", "금정구", "기장군", "남구", "동구", "동래구", "부산진구", "북구",
                "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구"
        ));

        // 대구광역시
        put("대구", Arrays.asList(
                "군위군", "남구", "달서구", "달성군", "동구", "북구", "서구", "수성구", "중구"
        ));

        // 인천광역시
        put("인천", Arrays.asList(
                "강화군", "계양구", "미추홀구", "남동구", "동구", "부평구", "서구", "연수구", "옹진군", "중구"
        ));

        // 광주광역시
        put("광주", Arrays.asList(
                "광산구", "남구", "동구", "북구", "서구"
        ));

        // 대전광역시
        put("대전", Arrays.asList(
                "대덕구", "동구", "서구", "유성구", "중구"
        ));

        // 울산광역시
        put("울산", Arrays.asList(
                "남구", "동구", "북구", "울주군", "중구"
        ));

        // 세종특별자치시
        put("세종", Arrays.asList("세종시"));

        // 경기도
        put("경기", Arrays.asList(
                "가평군", "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시",
                "남양주시", "동두천시", "부천시", "성남시", "수원시", "시흥시", "안산시", "안성시",
                "안양시", "양주시", "양평군", "여주시", "연천군", "오산시", "용인시", "의왕시",
                "의정부시", "이천시", "파주시", "평택시", "포천시", "하남시", "화성시"
        ));

        // 강원특별자치도
        put("강원", Arrays.asList(
                "강릉시", "고성군", "동해시", "삼척시", "속초시", "양구군", "양양군", "영월군",
                "원주시", "인제군", "정선군", "철원군", "춘천시", "태백시", "평창군", "홍천군", "화천군", "횡성군"
        ));

        // 충청북도
        put("충북", Arrays.asList(
                "괴산군", "단양군", "보은군", "영동군", "옥천군", "음성군", "제천시", "증평군", "진천군", "청주시", "충주시"
        ));

        // 충청남도
        put("충남", Arrays.asList(
                "계룡시", "공주시", "금산군", "논산시", "당진시", "보령시", "부여군", "서산시",
                "서천군", "아산시", "예산군", "천안시", "청양군", "태안군", "홍성군"
        ));

        // 전북특별자치도
        put("전북", Arrays.asList(
                "고창군", "군산시", "김제시", "남원시", "무주군", "부안군", "순창군", "완주군",
                "익산시", "임실군", "장수군", "전주시", "정읍시", "진안군"
        ));

        // 전라남도
        put("전남", Arrays.asList(
                "강진군", "고흥군", "곡성군", "광양시", "구례군", "나주시", "담양군", "목포시",
                "무안군", "보성군", "순천시", "신안군", "여수시", "영광군", "영암군", "완도군",
                "장성군", "장흥군", "진도군", "함평군", "해남군", "화순군"
        ));

        // 경상북도
        put("경북", Arrays.asList(
                "경산시", "경주시", "고령군", "구미시", "군위군", "김천시", "문경시", "봉화군",
                "상주시", "성주군", "안동시", "영덕군", "영양군", "영주시", "영천시", "예천군",
                "울릉군", "울진군", "의성군", "청도군", "청송군", "칠곡군", "포항시"
        ));

        // 경상남도
        put("경남", Arrays.asList(
                "거제시", "거창군", "고성군", "김해시", "남해군", "밀양시", "사천시", "산청군",
                "양산시", "의령군", "진주시", "창녕군", "창원시", "통영시", "하동군", "함안군",
                "함양군", "합천군"
        ));

        // 제주특별자치도
        put("제주", Arrays.asList("서귀포시", "제주시"));
    }};

    /**
     * 각 지역과 인접한 지역들의 매핑 (서울 중심)
     */
    private static final Map<String, List<String>> NEARBY_REGIONS = new HashMap<>() {{
        // 서울 내 인접 지역
        put("강남구", Arrays.asList("서초구", "송파구", "강동구"));
        put("서초구", Arrays.asList("강남구", "동작구", "관악구", "용산구"));
        put("송파구", Arrays.asList("강남구", "강동구", "광진구", "성동구"));
        put("강동구", Arrays.asList("강남구", "송파구", "광진구", "하남시", "구리시"));
        
        put("강서구", Arrays.asList("양천구", "구로구", "은평구", "김포시"));
        put("양천구", Arrays.asList("강서구", "구로구", "영등포구", "마포구"));
        put("구로구", Arrays.asList("강서구", "양천구", "영등포구", "금천구", "관악구", "광명시"));
        put("금천구", Arrays.asList("구로구", "영등포구", "관악구", "동작구", "안양시"));
        
        put("노원구", Arrays.asList("도봉구", "강북구", "중랑구", "성북구", "의정부시"));
        put("도봉구", Arrays.asList("노원구", "강북구", "성북구", "의정부시"));
        put("강북구", Arrays.asList("노원구", "도봉구", "성북구", "종로구"));
        put("성북구", Arrays.asList("노원구", "도봉구", "강북구", "종로구", "동대문구", "중랑구"));
        
        put("광진구", Arrays.asList("동대문구", "성동구", "송파구", "강동구", "중랑구", "구리시"));
        put("성동구", Arrays.asList("광진구", "송파구", "강남구", "용산구", "중구", "동대문구"));
        put("동대문구", Arrays.asList("성북구", "중랑구", "광진구", "성동구", "중구", "종로구"));
        put("중랑구", Arrays.asList("노원구", "성북구", "동대문구", "광진구", "구리시"));
        
        put("은평구", Arrays.asList("서대문구", "종로구", "강북구", "마포구", "고양시"));
        put("서대문구", Arrays.asList("은평구", "마포구", "종로구", "중구", "용산구"));
        put("마포구", Arrays.asList("은평구", "서대문구", "용산구", "영등포구", "양천구", "강서구", "고양시"));
        
        put("종로구", Arrays.asList("중구", "성북구", "강북구", "은평구", "서대문구", "용산구"));
        put("중구", Arrays.asList("종로구", "용산구", "성동구", "동대문구", "서대문구"));
        put("용산구", Arrays.asList("중구", "종로구", "서대문구", "마포구", "영등포구", "동작구", "서초구", "성동구"));
        
        put("영등포구", Arrays.asList("마포구", "용산구", "동작구", "관악구", "구로구", "양천구", "금천구"));
        put("동작구", Arrays.asList("용산구", "서초구", "관악구", "영등포구", "금천구"));
        put("관악구", Arrays.asList("동작구", "서초구", "금천구", "구로구", "영등포구", "안양시", "과천시"));

        // 경기도와 서울 경계 지역
        put("고양시", Arrays.asList("은평구", "마포구", "강서구", "파주시"));
        put("구리시", Arrays.asList("광진구", "강동구", "중랑구", "남양주시"));
        put("하남시", Arrays.asList("강동구", "송파구", "광주시", "성남시"));
        put("과천시", Arrays.asList("관악구", "서초구", "안양시"));
        put("광명시", Arrays.asList("구로구", "금천구", "안양시", "부천시"));
        put("성남시", Arrays.asList("강남구", "서초구", "송파구", "하남시", "광주시", "용인시"));
    }};

    /**
     * 주어진 텍스트에서 광역시/도를 추출합니다.
     */
    public static String extractProvince(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String normalizedText = text.trim();
        
        // 광역시/도 찾기
        for (String province : PROVINCES) {
            if (normalizedText.contains(province)) {
                // 정규화된 이름 반환 (서울특별시 -> 서울)
                if (province.contains("서울")) return "서울";
                if (province.contains("부산")) return "부산";
                if (province.contains("대구")) return "대구";
                if (province.contains("인천")) return "인천";
                if (province.contains("광주")) return "광주";
                if (province.contains("대전")) return "대전";
                if (province.contains("울산")) return "울산";
                if (province.contains("세종")) return "세종";
                if (province.contains("경기")) return "경기";
                if (province.contains("강원")) return "강원";
                if (province.contains("충청북") || province.equals("충북")) return "충북";
                if (province.contains("충청남") || province.equals("충남")) return "충남";
                if (province.contains("전라북") || province.equals("전북")) return "전북";
                if (province.contains("전라남") || province.equals("전남")) return "전남";
                if (province.contains("경상북") || province.equals("경북")) return "경북";
                if (province.contains("경상남") || province.equals("경남")) return "경남";
                if (province.contains("제주")) return "제주";
            }
        }

        return null;
    }

    /**
     * 주어진 텍스트에서 시/군/구를 추출합니다.
     */
    public static String extractDistrict(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String normalizedText = text.trim();
        
        // 모든 시/군/구에서 찾기
        for (List<String> districts : REGION_HIERARCHY.values()) {
            for (String district : districts) {
                if (normalizedText.contains(district)) {
                    return district;
                }
            }
        }

        return null;
    }

    /**
     * 주어진 텍스트에서 "광역시/도 + 시/군/구" 형태의 지역 정보를 추출합니다.
     * @param text 검색할 텍스트 (주소, 위치 정보 등)
     * @return "광역시/도 시/군/구" 형식 (예: "서울 강남구", "경기 수원시")
     */
    public static String extractRegion(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // 공백 정규화 (여러 공백을 하나로)
        String normalizedText = text.trim().replaceAll("\\s+", " ");

        String province = extractProvince(normalizedText);
        String district = extractDistrict(normalizedText);

        if (province != null && district != null) {
            return province + " " + district;
        } else if (district != null) {
            // 광역시/도를 찾지 못했지만 시/군/구가 있으면, 어느 광역시/도에 속하는지 찾기
            for (Map.Entry<String, List<String>> entry : REGION_HIERARCHY.entrySet()) {
                if (entry.getValue().contains(district)) {
                    return entry.getKey() + " " + district;
                }
            }
            return district;
        } else if (province != null) {
            return province;
        }

        return null;
    }

    /**
     * 선택된 지역과 인접한 지역들을 포함한 검색 범위를 반환합니다.
     */
    public static List<String> getSearchableRegions(String selectedRegion) {
        if (selectedRegion == null || selectedRegion.trim().isEmpty() || "전체".equals(selectedRegion)) {
            return Collections.emptyList();
        }

        List<String> searchableRegions = new ArrayList<>();
        searchableRegions.add(selectedRegion);

        // "광역시/도 시/군/구" 형식인 경우 시/군/구만 추출
        String district = selectedRegion;
        if (selectedRegion.contains(" ")) {
            String[] parts = selectedRegion.split(" ");
            district = parts[parts.length - 1];
        }

        // 인접 지역 추가
        List<String> nearbyRegions = NEARBY_REGIONS.get(district);
        if (nearbyRegions != null) {
            for (String nearby : nearbyRegions) {
                // 같은 광역시/도 내의 지역이면 전체 형식으로 추가
                if (selectedRegion.contains(" ")) {
                    String province = selectedRegion.split(" ")[0];
                    searchableRegions.add(province + " " + nearby);
                } else {
                    searchableRegions.add(nearby);
                }
            }
        }

        return searchableRegions;
    }

    /**
     * 역 이름에서 지역을 매핑합니다.
     */
    private static final Map<String, String> STATION_TO_REGION = new HashMap<>() {{
        // 서울 주요역
        put("용산역", "서울 용산구");
        put("서울역", "서울 용산구");
        put("영등포역", "서울 영등포구");
        put("신도림역", "서울 구로구");
        put("강남역", "서울 강남구");
        put("잠실역", "서울 송파구");
        put("신림역", "서울 관악구");
        put("수유역", "서울 강북구");
        put("노원역", "서울 노원구");
        put("왕십리역", "서울 성동구");
        put("건대입구역", "서울 광진구");
        put("홍대입구역", "서울 마포구");
        put("신촌역", "서울 서대문구");
        
        // 경기도 주요역
        put("수원역", "경기 수원시");
        put("성남역", "경기 성남시");
        put("안양역", "경기 안양시");
        put("부천역", "경기 부천시");
        put("의정부역", "경기 의정부시");
        put("광명역", "경기 광명시");
        put("평택역", "경기 평택시");
        put("안산역", "경기 안산시");
        put("고양역", "경기 고양시");
        put("병점역", "경기 화성시");
        put("오산역", "경기 오산시");
        put("동탄역", "경기 화성시");
        put("행신역", "경기 고양시");
        put("대화역", "경기 고양시");
        put("소요산역", "경기 동두천시");
        put("오이도역", "경기 시흥시");
        
        // 부산 주요역
        put("부전역", "부산 부산진구");
        put("구포역", "부산 북구");
        put("해운대역", "부산 해운대구");
        put("서면역", "부산 부산진구");
        
        // 기타 지역
        put("대전역", "대전 동구");
        put("천안역", "충남 천안시");
        put("익산역", "전북 익산시");
        put("포항역", "경북 포항시");
        put("오송역", "충북 청주시");
    }};

    /**
     * 역 이름에서 지역을 추출합니다.
     */
    public static String extractRegionFromStation(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String normalizedText = text.trim();
        for (Map.Entry<String, String> entry : STATION_TO_REGION.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * location, storageLocation, title에서 지역 정보를 추출합니다.
     */
    public static String extractRegionFromLocations(String location, String storageLocation) {
        // 1순위: location에서 직접 추출
        String region = extractRegion(location);
        if (region != null) {
            return region;
        }
        
        // 2순위: storageLocation에서 직접 추출
        region = extractRegion(storageLocation);
        if (region != null) {
            return region;
        }
        
        // 3순위: location이 역 이름인 경우
        region = extractRegionFromStation(location);
        if (region != null) {
            return region;
        }
        
        // 4순위: storageLocation이 역 이름인 경우
        region = extractRegionFromStation(storageLocation);
        if (region != null) {
            return region;
        }
        
        return null;
    }
    
    /**
     * title, location, storageLocation 모두에서 지역 정보를 추출합니다.
     */
    public static String extractRegionFromAll(String title, String location, String storageLocation) {
        // 1순위: title에서 추출 (가장 정확)
        String region = extractRegion(title);
        if (region != null) {
            return region;
        }
        
        // 2순위: location/storageLocation에서 추출
        return extractRegionFromLocations(location, storageLocation);
    }

    /**
     * 특정 광역시/도의 모든 시/군/구 목록을 반환합니다.
     */
    public static List<String> getDistrictsByProvince(String province) {
        return REGION_HIERARCHY.getOrDefault(province, Collections.emptyList());
    }

    /**
     * 모든 광역시/도 목록을 반환합니다 (정규화된 이름)
     */
    public static List<String> getAllProvinces() {
        return Arrays.asList("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
                "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주");
    }
}
