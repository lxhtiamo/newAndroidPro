package com.linewell.lxhdemo.thirdAppUtil.model;

/**
 * 统一的定位信息实体类
 * 包含百度和高德定位的共同字段 + 差异冗余字段
 */
public class UnifiedLocationInfo {
    // ====================== 【共同字段】百度&高德都具备 ======================
    private double latitude; // 纬度
    private double longitude; // 经度
    private float accuracy; // 定位精度（单位：米）：百度=getRadius()，高德=getAccuracy()
    private String address; // 详细地址：百度=getAddrStr()，高德=getAddress()
    private String country; // 国家
    private String province; // 省份
    private String city; // 城市
    private String district; // 区县
    private String street; // 街道
    private String adCode; // 区域编码
    private String buildingId; // 室内定位建筑物ID：百度=getBuildingID()，高德=getBuildingId()
    private String floor; // 室内定位楼层：百度=getFloor()，高德=getFloor()
    private String poiName; // POI名称：百度=Poi.getName()，高德=getPoiName()

    // ====================== 【百度特有字段】高德无，保留冗余 ======================
    private String town; // 乡镇信息：百度=getTown()
    private String coorType; // 经纬度坐标类型：百度=getCoorType()（如BD09LL）
    private String locationDescribe; // 位置描述信息：百度=getLocationDescribe()（如“在北京天安门附近”）
    private String buildingName; // 百度内部建筑物名称：百度=getBuildingName()
    private String poiAddr; // POI地址：百度=Poi.getAddr()
    private String poiTags; // POI类型标签：百度=Poi.getTag()
    private String poiRegionDerectionDesc; // POI区域位置关系：百度=PoiRegion.getDerectionDesc()
    private String poiRegionName; // POI区域名称：百度=PoiRegion.getName()
    private String poiRegionTags; // POI区域类型标签：百度=PoiRegion.getTags()

    // ====================== 【高德特有字段】百度无，保留冗余 ======================
    private double altitude; // 海拔（单位：米）：高德=getAltitude()
    private float speed; // 速度（单位：米/秒）：高德=getSpeed()
    private float bearing; // 方向角：高德=getBearing()
    private String streetNum; // 街道门牌号：高德=getStreetNum()
    private String cityCode; // 城市编码：高德=getCityCode()
    private String aoiName; // AOI名称：高德=getAoiName()
    private int gpsAccuracyStatus; // GPS当前状态：高德=getGpsAccuracyStatus()
    private int locationType; // 定位来源类型：高德=getLocationType()
    private String locationDetail; // 定位详细信息（排查问题用）：高德=getLocationDetail()

    // ====================== 【辅助字段】定位错误信息（统一处理失败场景） ======================
    private int errorCode; // 错误码：百度=getLocType()（失败时），高德=getErrorCode()
    private String errorInfo; // 错误信息：百度=定位失败描述，高德=getErrorInfo()

    // 无参构造
    public UnifiedLocationInfo() {
        // 初始化数值类型默认值
        this.altitude = 0.0d;
        this.speed = 0.0f;
        this.bearing = 0.0f;
        this.gpsAccuracyStatus = 0;
        this.locationType = 0;
        this.errorCode = 0;
    }

    // ====================== Getter & Setter （手动生成，也可使用Lombok的@Data注解简化） ======================
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getAdCode() {
        return adCode;
    }

    public void setAdCode(String adCode) {
        this.adCode = adCode;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getPoiName() {
        return poiName;
    }

    public void setPoiName(String poiName) {
        this.poiName = poiName;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCoorType() {
        return coorType;
    }

    public void setCoorType(String coorType) {
        this.coorType = coorType;
    }

    public String getLocationDescribe() {
        return locationDescribe;
    }

    public void setLocationDescribe(String locationDescribe) {
        this.locationDescribe = locationDescribe;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getPoiAddr() {
        return poiAddr;
    }

    public void setPoiAddr(String poiAddr) {
        this.poiAddr = poiAddr;
    }

    public String getPoiTags() {
        return poiTags;
    }

    public void setPoiTags(String poiTags) {
        this.poiTags = poiTags;
    }

    public String getPoiRegionDerectionDesc() {
        return poiRegionDerectionDesc;
    }

    public void setPoiRegionDerectionDesc(String poiRegionDerectionDesc) {
        this.poiRegionDerectionDesc = poiRegionDerectionDesc;
    }

    public String getPoiRegionName() {
        return poiRegionName;
    }

    public void setPoiRegionName(String poiRegionName) {
        this.poiRegionName = poiRegionName;
    }

    public String getPoiRegionTags() {
        return poiRegionTags;
    }

    public void setPoiRegionTags(String poiRegionTags) {
        this.poiRegionTags = poiRegionTags;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public String getStreetNum() {
        return streetNum;
    }

    public void setStreetNum(String streetNum) {
        this.streetNum = streetNum;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getAoiName() {
        return aoiName;
    }

    public void setAoiName(String aoiName) {
        this.aoiName = aoiName;
    }

    public int getGpsAccuracyStatus() {
        return gpsAccuracyStatus;
    }

    public void setGpsAccuracyStatus(int gpsAccuracyStatus) {
        this.gpsAccuracyStatus = gpsAccuracyStatus;
    }

    public int getLocationType() {
        return locationType;
    }

    public void setLocationType(int locationType) {
        this.locationType = locationType;
    }

    public String getLocationDetail() {
        return locationDetail;
    }

    public void setLocationDetail(String locationDetail) {
        this.locationDetail = locationDetail;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    // 可选：重写toString()，方便日志打印
    @Override
    public String toString() {
        return "UnifiedLocationInfo{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", address='" + address + '\'' +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street='" + street + '\'' +
                ", adCode='" + adCode + '\'' +
                ", buildingId='" + buildingId + '\'' +
                ", floor='" + floor + '\'' +
                ", poiName='" + poiName + '\'' +
                ", town='" + town + '\'' +
                ", coorType='" + coorType + '\'' +
                ", altitude=" + altitude +
                ", speed=" + speed +
                ", aoiName='" + aoiName + '\'' +
                '}';
    }
}