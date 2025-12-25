package com.linewell.lxhdemo.thirdAppUtil.util;

import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.baidu.location.PoiRegion;
import com.amap.api.location.AMapLocation;
import com.linewell.lxhdemo.thirdAppUtil.model.LocationInfo;

/**
 * 定位对象转换工具类：将百度/高德定位对象转换成统一的UnifiedLocationInfo
 */
public class LocationConvertUtils {

    /**
     * 将百度BDLocation转换为统一的UnifiedLocationInfo
     * @param bdLocation 百度定位结果对象
     * @return 统一定位信息对象
     */
    public static LocationInfo convertBDLocationToUnified(BDLocation bdLocation) {
        LocationInfo unifiedInfo = new LocationInfo();
        if (bdLocation == null) {
            return unifiedInfo;
        }

        // ====================== 【共同字段】赋值 ======================
        unifiedInfo.setLatitude(bdLocation.getLatitude());
        unifiedInfo.setLongitude(bdLocation.getLongitude());
        unifiedInfo.setAccuracy(bdLocation.getRadius()); // 百度的radius对应高德的accuracy
        unifiedInfo.setAddress(bdLocation.getAddrStr());
        unifiedInfo.setCountry(bdLocation.getCountry());
        unifiedInfo.setProvince(bdLocation.getProvince());
        unifiedInfo.setCity(bdLocation.getCity());
        unifiedInfo.setDistrict(bdLocation.getDistrict());
        unifiedInfo.setStreet(bdLocation.getStreet());
        unifiedInfo.setAdCode(bdLocation.getAdCode());
        unifiedInfo.setBuildingId(bdLocation.getBuildingID());
        unifiedInfo.setFloor(bdLocation.getFloor());

        // POI名称：处理PoiList为空的情况
        if (bdLocation.getPoiList() != null && !bdLocation.getPoiList().isEmpty()) {
            Poi firstPoi = bdLocation.getPoiList().get(0);
            unifiedInfo.setPoiName(firstPoi.getName());
            unifiedInfo.setPoiAddr(firstPoi.getAddr());
            unifiedInfo.setPoiTags(firstPoi.getTags());
        } else {
            unifiedInfo.setPoiName(null);
        }

        // ====================== 【百度特有字段】赋值 ======================
        unifiedInfo.setTown(bdLocation.getTown());
        unifiedInfo.setCoorType(bdLocation.getCoorType());
        unifiedInfo.setLocationDescribe(bdLocation.getLocationDescribe());
        unifiedInfo.setBuildingName(bdLocation.getBuildingName());

        // POI区域信息：处理PoiRegion为空的情况
        PoiRegion poiRegion = bdLocation.getPoiRegion();
        if (poiRegion != null) {
            unifiedInfo.setPoiRegionDerectionDesc(poiRegion.getDerectionDesc());
            unifiedInfo.setPoiRegionName(poiRegion.getName());
            unifiedInfo.setPoiRegionTags(poiRegion.getTags());
        }

        // 错误信息（定位失败时赋值）
        unifiedInfo.setErrorCode(bdLocation.getLocType()); // 百度的LocType作为错误码（成功时为对应类型，失败时为错误码）
        if (bdLocation.getLocType() == BDLocation.TypeServerError) {
            unifiedInfo.setErrorInfo("服务端错误");
        } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkException) {
            unifiedInfo.setErrorInfo("网络异常");
        } else if (bdLocation.getLocType() == BDLocation.TypeCriteriaException) {
            unifiedInfo.setErrorInfo("无法获取定位信号");
        } else {
            unifiedInfo.setErrorInfo(null);
        }

        return unifiedInfo;
    }

    /**
     * 将高德AMapLocation转换为统一的UnifiedLocationInfo
     * @param aMapLocation 高德定位结果对象
     * @return 统一定位信息对象
     */
    public static LocationInfo convertAMapLocationToUnified(AMapLocation aMapLocation) {
        LocationInfo unifiedInfo = new LocationInfo();
        if (aMapLocation == null) {
            return unifiedInfo;
        }

        // ====================== 【共同字段】赋值 ======================
        unifiedInfo.setLatitude(aMapLocation.getLatitude());
        unifiedInfo.setLongitude(aMapLocation.getLongitude());
        unifiedInfo.setAccuracy(aMapLocation.getAccuracy());
        unifiedInfo.setAddress(aMapLocation.getAddress());
        unifiedInfo.setCountry(aMapLocation.getCountry());
        unifiedInfo.setProvince(aMapLocation.getProvince());
        unifiedInfo.setCity(aMapLocation.getCity());
        unifiedInfo.setDistrict(aMapLocation.getDistrict());
        unifiedInfo.setStreet(aMapLocation.getStreet());
        unifiedInfo.setAdCode(aMapLocation.getAdCode());
        unifiedInfo.setBuildingId(aMapLocation.getBuildingId());
        unifiedInfo.setFloor(aMapLocation.getFloor());
        unifiedInfo.setPoiName(aMapLocation.getPoiName());

        // ====================== 【高德特有字段】赋值 ======================
        unifiedInfo.setAltitude(aMapLocation.getAltitude());
        unifiedInfo.setSpeed(aMapLocation.getSpeed());
        unifiedInfo.setBearing(aMapLocation.getBearing());
        unifiedInfo.setStreetNum(aMapLocation.getStreetNum());
        unifiedInfo.setCityCode(aMapLocation.getCityCode());
        unifiedInfo.setAoiName(aMapLocation.getAoiName());
        unifiedInfo.setGpsAccuracyStatus(aMapLocation.getGpsAccuracyStatus());
        unifiedInfo.setLocationType(aMapLocation.getLocationType());
        unifiedInfo.setLocationDetail(aMapLocation.getLocationDetail());

        // 错误信息（定位失败时赋值）
        unifiedInfo.setErrorCode(aMapLocation.getErrorCode());
        unifiedInfo.setErrorInfo(aMapLocation.getErrorInfo());

        return unifiedInfo;
    }
}