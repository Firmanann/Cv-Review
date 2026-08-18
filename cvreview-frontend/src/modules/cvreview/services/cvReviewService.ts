import { API_BASE_URL } from '@/config/api';
import { AnalysisResponse } from '../types/cvReview.types';

// Send CV file + job description to backend for analysis
export async function analyzeCv(
  cvFile: File,
  jobDescription: string
): Promise<AnalysisResponse> {
  const formData = new FormData();
  formData.append('cvFile', cvFile);
  formData.append('jobDescription', jobDescription);

  const response = await fetch(`${API_BASE_URL}/api/cv/analyze`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'An unexpected error occurred' }));
    throw new Error(errorData.message || `Request failed with status ${response.status}`);
  }

  return response.json();
}
