'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'next/navigation';
import api from '@/lib/api';

interface VaultFile {
  id: number;
  projectId: number;
  uploadedById: number;
  fileName: string;
  fileSize: number;
  mimeType: string | null;
  sha256Hash: string;
  version: number;
  duplicate: boolean;
  createdAt: string;
}

interface VerifyResult {
  vaultId: number;
  fileName: string;
  expectedHash: string;
  actualHash: string;
  intact: boolean;
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function VaultPage() {
  const params = useParams();
  const projectId = params.id as string;
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [files, setFiles] = useState<VaultFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [verifyResults, setVerifyResults] = useState<Record<number, VerifyResult>>({});
  const [verifying, setVerifying] = useState<number | null>(null);

  const fetchFiles = useCallback(async () => {
    try {
      const res = await api.get(`/projects/${projectId}/files`);
      setFiles(res.data);
    } catch (e) {
      console.error('Failed to fetch files', e);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  const uploadFile = async (file: File) => {
    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await api.post(`/projects/${projectId}/files`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      await fetchFiles();
    } catch (e) {
      console.error('Upload failed', e);
      alert('업로드 실패');
    } finally {
      setUploading(false);
    }
  };

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) await uploadFile(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) await uploadFile(file);
  };

  const handleVerify = async (vaultId: number) => {
    setVerifying(vaultId);
    try {
      const res = await api.get(`/files/${vaultId}/verify`);
      setVerifyResults(prev => ({ ...prev, [vaultId]: res.data }));
    } catch (e) {
      console.error('Verify failed', e);
    } finally {
      setVerifying(null);
    }
  };

  const handleDownload = (vaultId: number, fileName: string) => {
    const token = document.cookie; // fallback; actual token via interceptor
    const link = document.createElement('a');
    link.href = `/api/files/${vaultId}/download`;
    link.download = fileName;
    link.click();
  };

  if (loading) return <div className="p-8 text-slate-400">불러오는 중...</div>;

  return (
    <div className="flex flex-col h-full overflow-y-auto custom-scrollbar p-6">
      {/* Header */}
      <div className="flex shrink-0 items-center justify-between backdrop-blur-md bg-slate-900/40 p-5 rounded-2xl mb-6 border border-slate-800 shadow-xl">
        <div>
          <h2 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-amber-400 to-orange-400">
            Hash Vault
          </h2>
          <p className="text-sm text-slate-400 mt-1">모든 파일은 SHA-256 해시로 고정됩니다. 변조 시 즉시 감지됩니다.</p>
        </div>
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg transition-all hover:-translate-y-0.5 disabled:opacity-50"
        >
          {uploading ? '업로드 중...' : '+ 파일 업로드'}
        </button>
        <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileSelect} />
      </div>

      {/* Drag & Drop Zone */}
      <div
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`mb-6 border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-all
          ${dragging
            ? 'border-amber-500 bg-amber-500/10 text-amber-300'
            : 'border-slate-700 hover:border-amber-500/50 hover:bg-slate-800/30 text-slate-500 hover:text-slate-400'
          }`}
      >
        <div className="text-4xl mb-2">🔒</div>
        <p className="font-medium">파일을 드래그하거나 클릭해서 업로드하세요</p>
        <p className="text-xs mt-1 opacity-70">업로드 즉시 SHA-256 해시가 고정되며, 이후 변조 여부를 검증할 수 있습니다.</p>
      </div>

      {/* File List */}
      {files.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-40 gap-3">
          <p className="text-slate-500">아직 업로드된 파일이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {files.map(file => {
            const verify = verifyResults[file.id];
            return (
              <div key={file.id} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5">
                <div className="flex items-start gap-4">
                  {/* File Icon */}
                  <div className="w-10 h-10 bg-amber-500/10 rounded-xl flex items-center justify-center shrink-0 text-xl">
                    📄
                  </div>

                  {/* File Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold text-slate-100 truncate">{file.fileName}</span>
                      <span className="text-xs bg-slate-800 text-slate-400 px-2 py-0.5 rounded-md">
                        v{file.version}
                      </span>
                      {file.duplicate && (
                        <span className="text-xs bg-yellow-900/40 text-yellow-400 border border-yellow-500/30 px-2 py-0.5 rounded-md">
                          중복 해시
                        </span>
                      )}
                      {verify && (
                        <span className={`text-xs px-2 py-0.5 rounded-md border font-bold
                          ${verify.intact
                            ? 'bg-emerald-900/40 text-emerald-400 border-emerald-500/30'
                            : 'bg-rose-900/40 text-rose-400 border-rose-500/30'
                          }`}>
                          {verify.intact ? '✓ 무결' : '⚠ 변조 감지'}
                        </span>
                      )}
                    </div>

                    <div className="mt-1 flex items-center gap-3 text-xs text-slate-500 flex-wrap">
                      <span>{formatBytes(file.fileSize)}</span>
                      <span>·</span>
                      <span>{formatDate(file.createdAt)}</span>
                      <span>·</span>
                      <span className="font-mono text-slate-600 truncate max-w-[240px]"
                            title={file.sha256Hash}>
                        {file.sha256Hash.slice(0, 16)}…
                      </span>
                    </div>

                    {/* Verify detail */}
                    {verify && !verify.intact && (
                      <div className="mt-2 p-3 bg-rose-900/20 border border-rose-500/20 rounded-xl text-xs space-y-1">
                        <p className="text-rose-300 font-bold">변조가 감지되었습니다!</p>
                        <p className="text-slate-400 font-mono">기대: {verify.expectedHash.slice(0, 32)}…</p>
                        <p className="text-slate-400 font-mono">실제: {verify.actualHash.slice(0, 32)}…</p>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => handleVerify(file.id)}
                      disabled={verifying === file.id}
                      className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-lg transition-all disabled:opacity-50"
                    >
                      {verifying === file.id ? '검증 중...' : '검증'}
                    </button>
                    <button
                      onClick={() => handleDownload(file.id, file.fileName)}
                      className="px-3 py-1.5 bg-amber-600/20 hover:bg-amber-600/40 text-amber-400 text-xs font-semibold rounded-lg border border-amber-500/20 transition-all"
                    >
                      다운로드
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <style dangerouslySetInnerHTML={{__html: `
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51,65,85,0.5); border-radius: 8px; }
      `}} />
    </div>
  );
}
