"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Info,
  FileCheck,
  History,
  Code,
  Download,
  Upload,
  ChevronDown,
  Loader2,
  AlertCircle,
} from "lucide-react";
import UploadVersionModal from "./UploadVersionModal";
import Toast from "@/components/ui/Toast";
import { documentTypeLabel, versionStatusBadge, documentStatusBadge } from "@/constants/document";
import { formatBytes, formatDateTimeSlash, formatDateShort } from "@/utils/format";
import { getDocument, downloadVersion } from "@/services/documentService";
import DocumentSharePanel from "./DocumentSharePanel";

function Field({ label, children }) {
  return (
    <div>
      <label className="block mb-1.5 text-sm font-bold text-text-primary">{label}</label>
      <p className="text-sm text-text-secondary">{children ?? "—"}</p>
    </div>
  );
}

export default function DocumentDetailView({ documentId }) {
  const router = useRouter();
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [versionOpen, setVersionOpen] = useState(false);
  const [toast, setToast] = useState(null);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getDocument(documentId, controller.signal)
      .then((d) => {
        if (!controller.signal.aborted) setDetail(d);
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message || "Failed to load document");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [documentId, reloadKey]);

  const download = async (versionId, fileName) => {
    try {
      await downloadVersion(documentId, versionId, fileName);
    } catch (err) {
      setToast({ type: "error", text: err.message || "Download failed" });
    }
  };

  const Header = (
    <div className="flex items-center justify-between mb-8">
      <div>
        <h1 className="text-2xl font-bold text-text-primary tracking-tight">Document Details</h1>
        <p className="text-sm text-text-muted mt-1">
          View and manage document information, versions, and metadata
        </p>
      </div>
      {/* router.back(), not a fixed href: the row that opened this page pushed a
          real history entry for its folder (see FileStorageView's openFolder),
          so browser Back lands back in that folder instead of File Storage root. */}
      <button type="button" onClick={() => router.back()} className="btn-secondary py-2 px-4 text-sm">
        <ArrowLeft className="h-4 w-4" />
        Back
      </button>
    </div>
  );

  if (loading) {
    return (
      <main className="flex-1 mx-auto w-full max-w-[1200px] px-4 py-8 sm:px-6 lg:px-8">
        {Header}
        <div className="flex items-center justify-center gap-2 py-24 text-text-muted">
          <Loader2 className="h-6 w-6 animate-spin" />
          <span className="text-sm">Loading document…</span>
        </div>
      </main>
    );
  }

  if (error || !detail) {
    return (
      <main className="flex-1 mx-auto w-full max-w-[1200px] px-4 py-8 sm:px-6 lg:px-8">
        {Header}
        <div className="flex flex-col items-center justify-center gap-3 py-24 text-text-muted">
          <AlertCircle className="h-6 w-6 text-error" />
          <p className="text-sm text-error">{error || "Document not found"}</p>
        </div>
      </main>
    );
  }

  const { documentInfo: info, currentVersion: cur, versionHistory = [], advancedInfo: adv } = detail;
  const docStatus = documentStatusBadge(info?.status);
  const curStatus = versionStatusBadge(cur?.versionStatus);

  return (
    <main className="flex-1 mx-auto w-full max-w-[1200px] px-4 py-8 sm:px-6 lg:px-8">
      {Header}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Document Information */}
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10">
              <Info className="h-4 w-4 text-accent" />
            </span>
            <h2 className="text-base font-semibold text-text-primary">Document Information</h2>
          </div>
          <div className="space-y-4">
            <Field label="Title">{info?.title}</Field>
            <Field label="Description">{info?.description}</Field>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Document Type</label>
                <span className="badge badge-neutral">{documentTypeLabel(info?.documentType)}</span>
              </div>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Status</label>
                <span className={`badge ${docStatus.badge}`}>{docStatus.label}</span>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Created At">{formatDateTimeSlash(info?.createdAt)}</Field>
              <Field label="Updated At">{formatDateTimeSlash(info?.updatedAt)}</Field>
            </div>
          </div>
        </div>

        {/* Current Version */}
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-green-500/10">
              <FileCheck className="h-4 w-4 text-green-400" />
            </span>
            <h2 className="text-base font-semibold text-text-primary">Current Version</h2>
          </div>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Current Version">v{cur?.versionNumber}</Field>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Version Status</label>
                <span className={`badge ${curStatus.badge}`}>{curStatus.label}</span>
              </div>
            </div>
            <Field label="File Name">{cur?.fileName}</Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="File Size">{formatBytes(cur?.fileSize)}</Field>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Extension</label>
                <span className="text-sm text-text-secondary uppercase">{cur?.extension || "—"}</span>
              </div>
            </div>
            <Field label="Uploaded Date">{formatDateTimeSlash(cur?.uploadedDate)}</Field>

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                className="btn-primary py-2 px-4 text-sm"
                onClick={() => download(cur?.versionId, cur?.fileName)}
                disabled={!cur?.versionId}
              >
                <Download className="h-4 w-4" />
                Download
              </button>
              <button
                type="button"
                className="btn-secondary py-2 px-4 text-sm"
                onClick={() => setVersionOpen(true)}
              >
                <Upload className="h-4 w-4" />
                Upload New Version
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ponytail: DocumentSharePanel temporarily hidden per request, re-enable by uncommenting
      <div className="mb-6">
        <DocumentSharePanel documentId={documentId} ownerId={info.ownerId} onToast={setToast} />
      </div>
      */}

      {/* Version History */}
      <div className="card overflow-hidden mb-6">
        <div className="flex items-center gap-2 px-6 py-4 border-b border-border-subtle">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-500/10">
            <History className="h-3.5 w-3.5 text-blue-400" />
          </span>
          <h2 className="text-base font-semibold text-text-primary">Version History</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[800px]">
            <thead>
              <tr className="border-b border-border-subtle bg-bg-secondary">
                {["Version", "File Name", "Change Note", "Status", "Created At", "Action"].map((h) => (
                  <th key={h} className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-text-muted">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {versionHistory.map((v) => {
                const st = versionStatusBadge(v.status);
                const isCurrent = v.versionNumber === cur?.versionNumber;
                return (
                  <tr key={v.versionId} className="border-b border-border-subtle transition-colors hover:bg-bg-elevated/50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-text-primary">v{v.versionNumber}</span>
                        {isCurrent && (
                          <span className="badge badge-success text-[10px] px-1.5 py-0">Current</span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-text-secondary">{v.fileName}</td>
                    <td className="px-6 py-4 text-sm text-text-secondary max-w-[220px] break-words whitespace-normal">
                      {v.changeNote || "—"}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`badge ${st.badge}`}>{st.label}</span>
                    </td>
                    <td className="px-6 py-4 text-sm text-text-secondary whitespace-nowrap">
                      {formatDateShort(v.createdAt)}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          className="btn-ghost p-1.5 text-xs"
                          aria-label="Download"
                          onClick={() => download(v.versionId, v.fileName)}
                        >
                          <Download className="h-4 w-4" />
                          <span className="sr-only">Download</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Advanced Information */}
      <div className="card overflow-hidden">
        <button
          type="button"
          className="flex items-center gap-3 w-full px-6 py-4 text-left transition-colors hover:bg-bg-elevated/50"
          onClick={() => setAdvancedOpen((v) => !v)}
        >
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-500/10">
            <Code className="h-3.5 w-3.5 text-amber-400" />
          </span>
          <span className="flex-1 text-base font-semibold text-text-primary">Advanced Information</span>
          <ChevronDown className={`collapse-chevron h-4 w-4 text-text-muted ${advancedOpen ? "rotated" : ""}`} />
        </button>
        <div className={`collapse-section px-6 pb-6 ${advancedOpen ? "" : "collapsed"}`}>
          <div className="border-t border-border-subtle pt-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field label="Storage Provider">{adv?.storageProvider}</Field>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Bucket Name</label>
                <p className="text-sm text-text-secondary font-mono">{adv?.bucketName || "—"}</p>
              </div>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Object Key</label>
                <p className="text-sm text-text-secondary font-mono text-xs break-all">{adv?.objectKey || "—"}</p>
              </div>
              <div>
                <label className="block mb-1.5 text-sm font-bold text-text-primary">Checksum</label>
                <p className="text-sm text-text-secondary font-mono break-all">{adv?.checksum || "—"}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <UploadVersionModal
        open={versionOpen}
        onClose={() => setVersionOpen(false)}
        documentId={documentId}
        title={info?.title}
        onUploaded={() => {
          setToast({ type: "success", text: "New version uploaded" });
          reload();
        }}
      />

      <Toast toast={toast} onDone={() => setToast(null)} />
    </main>
  );
}
