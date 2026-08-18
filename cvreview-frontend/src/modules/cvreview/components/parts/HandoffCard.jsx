'use client';

import { useState, useRef } from 'react';
import { FileText, ClipboardPaste, Upload, CheckCircle2, AlertCircle, ArrowRight } from 'lucide-react';

export default function HandoffCard({ onSubmit, isLoading }) {
  const [activeTab, setActiveTab] = useState('file'); // 'file' | 'job_desc'
  const [jobDescription, setJobDescription] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const maxChars = 50000;
  const isFileReady = selectedFile !== null;
  const isJobDescReady = jobDescription.trim().length >= 50;
  const canSubmit = isFileReady && isJobDescReady && !isLoading;

  // Handle file selection
  const handleFileSelect = (file) => {
    if (file && (file.type === 'application/pdf' || file.name.endsWith('.docx') || file.name.endsWith('.pdf'))) {
      setSelectedFile(file);
    }
  };

  // Handle form submission
  const handleSubmit = () => {
    if (canSubmit) {
      onSubmit(selectedFile, jobDescription.trim());
    }
  };

  return (
    <div>
      {/* Section label */}
      <div className="flex items-center justify-between mb-2">
        <span className="font-mono text-[10px] tracking-[0.2em] uppercase text-stone-500">
          Your Handoff
        </span>
      </div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-sm text-stone-600">
          Provide your CV file and target job description.
        </p>
        <CheckCircle2 className={`w-4 h-4 transition-colors ${canSubmit ? 'text-green-600' : 'text-stone-300'}`} />
      </div>

      {/* Card */}
      <div className="bg-[var(--card-white)] rounded-2xl border border-stone-200/60 shadow-sm overflow-hidden">
        {/* Tabs */}
        <div className="flex border-b border-stone-200/60 bg-stone-50/50">
          {/* Tab 1: CV File */}
          <button
            type="button"
            onClick={() => setActiveTab('file')}
            className={`flex-1 flex items-center justify-center gap-2 px-5 py-3.5 text-xs font-medium transition-colors border-r border-stone-200/60 ${
              activeTab === 'file'
                ? 'text-stone-800 bg-[var(--card-white)] border-b-2 border-b-[var(--accent-orange)] font-semibold shadow-xs'
                : 'text-stone-500 hover:text-stone-700 hover:bg-stone-100/50'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            <span>1. CV File</span>
            {isFileReady ? (
              <span className="w-2 h-2 rounded-full bg-green-500 ml-1" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-stone-300 ml-1" />
            )}
          </button>

          {/* Tab 2: Job Description */}
          <button
            type="button"
            onClick={() => setActiveTab('job_desc')}
            className={`flex-1 flex items-center justify-center gap-2 px-5 py-3.5 text-xs font-medium transition-colors ${
              activeTab === 'job_desc'
                ? 'text-stone-800 bg-[var(--card-white)] border-b-2 border-b-[var(--accent-orange)] font-semibold shadow-xs'
                : 'text-stone-500 hover:text-stone-700 hover:bg-stone-100/50'
            }`}
          >
            <ClipboardPaste className="w-3.5 h-3.5" />
            <span>2. Job Description</span>
            {isJobDescReady ? (
              <span className="w-2 h-2 rounded-full bg-green-500 ml-1" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-stone-300 ml-1" />
            )}
          </button>
        </div>

        <div className="p-6">
          {/* TAB 1 CONTENT: File Upload */}
          {activeTab === 'file' && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block font-mono text-[10px] tracking-[0.15em] uppercase text-stone-500">
                  Upload Resume / CV
                </label>
                <span className="text-[10px] font-mono text-stone-400">PDF or DOCX (Max 5MB)</span>
              </div>

              <div
                className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all ${
                  dragOver
                    ? 'border-[var(--accent-orange)] bg-orange-50/50 scale-[0.99]'
                    : isFileReady
                    ? 'border-green-400 bg-green-50/40'
                    : 'border-stone-200 hover:border-stone-300 bg-stone-50/30'
                }`}
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={(e) => {
                  e.preventDefault();
                  setDragOver(false);
                  handleFileSelect(e.dataTransfer.files[0]);
                }}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  className="hidden"
                  onChange={(e) => handleFileSelect(e.target.files?.[0])}
                />

                {isFileReady ? (
                  <div className="flex flex-col items-center gap-2">
                    <CheckCircle2 className="w-8 h-8 text-green-600" />
                    <span className="text-sm font-semibold text-green-800">{selectedFile.name}</span>
                    <span className="text-[11px] font-mono text-green-600">
                      {(selectedFile.size / 1024).toFixed(1)} KB • Ready for analysis
                    </span>
                    <span className="text-[10px] text-stone-400 mt-2 underline">
                      Click to choose a different file
                    </span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center">
                    <Upload className="w-7 h-7 text-stone-400 mb-2" />
                    <p className="text-sm font-medium text-stone-700">
                      Drop your CV here or click to browse
                    </p>
                    <p className="text-xs text-stone-400 font-mono mt-1">
                      Supports ATS compliant PDF and DOCX
                    </p>
                  </div>
                )}
              </div>

              {/* Next step hint */}
              <div className="mt-5 flex items-center justify-between pt-4 border-t border-stone-100">
                <p className="text-xs text-stone-500">
                  {isFileReady
                    ? '✓ CV ready! Next, add the target job description.'
                    : 'Step 1 of 2: Upload your CV file first.'}
                </p>
                <button
                  type="button"
                  onClick={() => setActiveTab('job_desc')}
                  className="flex items-center gap-1.5 text-xs font-mono uppercase tracking-wider text-[var(--accent-orange)] hover:underline font-semibold"
                >
                  <span>Go to Job Desc</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          )}

          {/* TAB 2 CONTENT: Job Description */}
          {activeTab === 'job_desc' && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block font-mono text-[10px] tracking-[0.15em] uppercase text-stone-500">
                  Target Job Description
                </label>
                <span className={`text-[10px] font-mono ${isJobDescReady ? 'text-green-600 font-medium' : 'text-amber-600'}`}>
                  {isJobDescReady ? '✓ Requirements met' : 'Min. 50 characters required'}
                </span>
              </div>

              <textarea
                value={jobDescription}
                onChange={(e) => setJobDescription(e.target.value.slice(0, maxChars))}
                placeholder="Paste the role qualifications, responsibilities, and requirements from the job posting..."
                rows={7}
                className="w-full bg-stone-50/50 rounded-xl p-3.5 border border-stone-200 text-sm text-stone-700 placeholder:text-stone-400 resize-none outline-none focus:border-[var(--accent-orange)] focus:bg-white transition-all leading-6 font-mono"
              />

              {/* Footer: char count */}
              <div className="flex items-center justify-between pt-3 mt-1 text-stone-400 text-[10px] font-mono">
                <span>
                  {jobDescription.length < 50
                    ? `${50 - jobDescription.length} more characters needed`
                    : 'Ready for ATS keyword matching'}
                </span>
                <span>
                  {jobDescription.length.toLocaleString()} / {maxChars.toLocaleString()}
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Bottom bar: checklist & submit */}
        <div className="flex flex-col sm:flex-row items-center justify-between px-6 py-4 border-t border-stone-100 bg-stone-50/40 gap-3">
          {/* Status checklist */}
          <div className="flex items-center gap-4 text-[11px] font-mono">
            <div className="flex items-center gap-1.5">
              {isFileReady ? (
                <CheckCircle2 className="w-3.5 h-3.5 text-green-600" />
              ) : (
                <AlertCircle className="w-3.5 h-3.5 text-stone-300" />
              )}
              <span className={isFileReady ? 'text-stone-700 font-medium' : 'text-stone-400'}>
                1. CV File
              </span>
            </div>
            <span className="text-stone-300">•</span>
            <div className="flex items-center gap-1.5">
              {isJobDescReady ? (
                <CheckCircle2 className="w-3.5 h-3.5 text-green-600" />
              ) : (
                <AlertCircle className="w-3.5 h-3.5 text-stone-300" />
              )}
              <span className={isJobDescReady ? 'text-stone-700 font-medium' : 'text-stone-400'}>
                2. Job Desc
              </span>
            </div>
          </div>

          {/* Submit button */}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!canSubmit}
            className={`flex items-center gap-2 px-6 py-2.5 rounded-full text-xs font-medium transition-all ${
              canSubmit
                ? 'bg-[var(--accent-orange)] text-white hover:brightness-110 active:scale-[0.98] shadow-sm'
                : 'bg-stone-200 text-stone-400 cursor-not-allowed'
            }`}
          >
            {isLoading ? (
              <>
                <span className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Analyzing...</span>
              </>
            ) : (
              <>
                <span>Send it on</span>
                <span>→</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
