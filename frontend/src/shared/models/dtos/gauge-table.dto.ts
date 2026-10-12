export interface GaugePointRequest {
  heightCm: number;
  volumeGallons: number;
}

export interface GaugeTableRequest {
  name: string;
  points: GaugePointRequest[];
}

export interface GaugeTableResponse {
  id: number;
  name: string;
  points: GaugePointRequest[];
  minHeightCm: number;
  maxHeightCm: number;
  maxVolumeGallons: number;
  tanks: Array<{ id: number; code: string; name: string }>;
}
