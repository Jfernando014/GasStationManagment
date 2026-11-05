import { vi } from 'vitest';
import { ApiError } from '../models/api-error.model';
import {
  DOWNLOAD_ERROR_MESSAGES,
  getDownloadErrorMessage,
  saveBlob
} from './file-download.helpers';
import { ReportAdaptationError } from './daily-report-adapter.helpers';

/**
 * @author javiersolanop777
 */
describe('file download helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  describe('saveBlob', () => {
    it('clicks a temporary link with the file name and frees the memory', () => {
      const objLinks: HTMLAnchorElement[] = [];
      URL.createObjectURL = vi.fn(() => 'blob:test');
      URL.revokeObjectURL = vi.fn();
      vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
        objLinks.push(this);
      });

      saveBlob(new Blob(['x']), 'planilla.xlsx');

      expect(objLinks).toHaveLength(1);
      expect(objLinks[0].download).toBe('planilla.xlsx');
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
      expect(document.querySelector('a[download]')).toBeNull();
    });
  });

  describe('getDownloadErrorMessage', () => {
    it('explains each HTTP status', () => {
      expect(getDownloadErrorMessage(new ApiError(404, {}))).toBe(DOWNLOAD_ERROR_MESSAGES.noData);
      expect(getDownloadErrorMessage(new ApiError(403, {}))).toBe(DOWNLOAD_ERROR_MESSAGES.forbidden);
      expect(getDownloadErrorMessage(new ApiError(401, {}))).toBe(DOWNLOAD_ERROR_MESSAGES.unauthorized);
      expect(getDownloadErrorMessage(new ApiError(400, {}))).toBe(DOWNLOAD_ERROR_MESSAGES.badRequest);
      expect(getDownloadErrorMessage(new ApiError(500, {}))).toBe(DOWNLOAD_ERROR_MESSAGES.default);
    });

    it('keeps the connection message of the interceptor', () => {
      expect(getDownloadErrorMessage(new ApiError(0, { detail: 'No se pudo conectar con el servidor' }))).toBe(
        'No se pudo conectar con el servidor'
      );
    });

    it('explains that the file of the server has an unexpected format', () => {
      expect(getDownloadErrorMessage(new ReportAdaptationError('Missing columns'))).toBe(
        DOWNLOAD_ERROR_MESSAGES.format
      );
    });

    it('uses the default message for unknown errors', () => {
      expect(getDownloadErrorMessage(new Error('boom'))).toBe(DOWNLOAD_ERROR_MESSAGES.default);
    });
  });
});
