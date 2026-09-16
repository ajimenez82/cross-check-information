import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';

export function Dialog({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="dialog" onCancel={onClose} onClick={e => { if (e.target === e.currentTarget) onClose(); }} aria-labelledby="dialog-title"><div className="dialogHead"><h2 id="dialog-title">{title}</h2><button className="iconButton" aria-label="Cerrar" onClick={onClose}><X /></button></div>{children}</dialog>;
}
