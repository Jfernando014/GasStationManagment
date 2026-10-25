import type PptxGenJS from 'pptxgenjs';
import { BasketStatisticsResponse } from '../../shared/models/dtos/basket-statistics.dto';
import { ExplicitStatisticsRequest, SalesStatisticsResponse } from '../../shared/models/dtos/sales-statistics.dto';
import { addExact, chartRatio, difference, formatStatistics } from '../../shared/helpers/sales-statistics.helpers';
import { basketSellers } from '../../shared/helpers/statistics-contracts.helpers';

export interface StatisticsBalance {
  month: string; sales: ExplicitStatisticsRequest; averages: SalesStatisticsResponse | null; basket: BasketStatisticsResponse | null;
}

/** Four real OOXML slides. Every figure retains its source; missing information remains explicit. */
export async function buildStatisticsBalance(data: StatisticsBalance): Promise<PptxGenJS> {
  const { default: Presentation } = await import('pptxgenjs');
  const pptx = new Presentation(); pptx.layout = 'LAYOUT_WIDE'; pptx.author = 'EDS Bellavista';
  pptx.subject = 'Estadísticas: observaciones proporcionadas y disponibilidad de ventas reales';
  pptx.title = 'Balance de Estadísticas · ' + data.month;
  const slides = ['Portada & Resumen de Despachos', 'Turnos Día vs. Noche', 'Ranking de Vendedores', 'Conclusiones y Recomendaciones'].map((title, index) => {
    const slide = pptx.addSlide(); slide.background = { color: 'F4F7FA' };
    slide.addShape(pptx.ShapeType.rect, { x: 0, y: 0, w: 13.333, h: 1.3, fill: { color: '0B192C' }, line: { color: '0B192C' } });
    slide.addText('EDS BELLAVISTA · ' + data.month, { x: .55, y: .2, w: 12, h: .25, color: 'FF6B00', fontSize: 12, bold: true });
    slide.addText(title, { x: .55, y: .65, w: 12, h: .4, color: 'FFFFFF', fontSize: 25, bold: true });
    slide.addText('Fuentes identificadas en cada sección · Sin cifras supuestas · ' + (index + 1) + '/4', { x: .55, y: 7.05, w: 12, h: .2, fontSize: 10, color: '64748B' });
    return slide;
  });
  const text = (slide: PptxGenJS.Slide, value: string, y: number, color = '334155', fontSize = 19): void => {
    slide.addText(value, { x: .65, y, w: 12, h: .65, fontSize, color, breakLine: false, valign: 'top' });
  };
  const unit = data.sales.category === 'BASKET' ? 'COP' : 'Gal';
  const total = data.sales.observations.reduce((sum, row) => addExact(sum, row.value), '0');
  text(slides[0], 'Período: ' + data.sales.from + ' a ' + data.sales.to, 1.65);
  text(slides[0], 'OBSERVACIONES PROPORCIONADAS POR EL CLIENTE', 2.5, 'C2410C', 15);
  text(slides[0], data.sales.observations.length ? formatStatistics(total) + ' ' + unit + ' · ' + data.sales.observations.length + ' totales fecha/jornada' : 'Sin observaciones proporcionadas para este período.', 3.1);
  text(slides[0], 'No constituyen ventas históricas verificadas ni constancia de asistencia.', 4, '64748B', 16);
  text(slides[0], 'Ventas reales de canastilla: ' + (data.basket?.charts.availability === 'AVAILABLE' && data.basket.summary ? formatStatistics(data.basket.summary.amount) + ' COP' : data.basket?.charts.availability === 'NO_DATA' ? 'sin registros para el período' : 'fuente indisponible'), 5.2);
  const summaries = data.averages?.series.filter(s => s.available && s.average !== null) ?? [];
  if (summaries.length) {
    const maximum = summaries.reduce((max, s) => difference(s.average!, max).startsWith('-') ? max : s.average!, '0');
    summaries.forEach((summary, index) => {
      const height = chartRatio(summary.average!, maximum) * 2.4, x = 3 + index * 4;
      slides[1].addShape(pptx.ShapeType.rect, { x, y: 5.7 - height, w: 1.2, h: Math.max(height, .005), fill: { color: index ? '0B192C' : '0284C7' }, line: { color: index ? '0B192C' : '0284C7' } });
      slides[1].addText(formatStatistics(summary.average) + ' ' + unit, { x: x - .7, y: 5.25 - height, w: 2.6, h: .35, align: 'center', fontSize: 18, bold: true });
      slides[1].addText(summary.label + ' · ' + summary.observations + ' observaciones', { x: x - 1, y: 5.9, w: 3.2, h: .5, align: 'center', fontSize: 15 });
    });
    text(slides[1], 'Promedios calculados por backend · Observaciones del cliente · HALF_EVEN', 1.65, '64748B', 15);
  } else text(slides[1], 'Sin observaciones suficientes para comparar día y noche. No se presentan promedios supuestos.', 2.5);
  const sellers = data.basket ? basketSellers(data.basket) : [];
  if (sellers.length) {
    slides[2].addTable([
      [{ text: 'Vendedor', options: { bold: true } }, { text: 'Venta real COP', options: { bold: true } }, { text: 'Turnos verificados', options: { bold: true } }],
      ...sellers.slice(0, 8).map(s => [s.name, formatStatistics(s.amount), s.workedShifts === null ? 'No disponible' : String(s.workedShifts)].map(text => ({ text })))
    ], { x: .65, y: 1.8, w: 12, colW: [6, 3, 3], fontSize: 14, border: { color: 'E2E8F0', pt: 1 }, fill: { color: 'FFFFFF' }, color: '0F172A', rowH: .42 });
    text(slides[2], 'Ventas históricas reales. Se muestran hasta 8 vendedores de ' + sellers.length + '. Sin incentivos o cumplimiento inferidos.', 6.15, '64748B', 13);
  } else text(slides[2], 'Ranking y canastilla por vendedor no disponibles: faltan registros históricos reales para este período.', 2.5);
  text(slides[3], 'Información disponible: ' + (data.sales.observations.length ? 'análisis de observaciones proporcionadas.' : 'registros reales de canastilla.'), 1.8);
  text(slides[3], 'Ventas históricas por vendedor: ' + (data.basket?.charts.availability ?? 'UNAVAILABLE'), 2.8);
  text(slides[3], 'Turnos efectivamente trabajados: ' + (data.basket?.workedShiftsAvailability ?? 'UNAVAILABLE'), 3.6);
  text(slides[3], 'No se emiten recomendaciones de inventario, metas o incentivos sin sus fuentes y reglas.', 4.55, '64748B', 18);
  text(slides[3], 'Las proyecciones y observaciones del cliente no representan ventas realizadas.', 5.65, 'C2410C', 17);
  return pptx;
}
