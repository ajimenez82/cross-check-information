import { useEffect, useRef, useState } from 'react';
import { ChevronDown, LogOut, Settings2, UserRound } from 'lucide-react';
import { demoUser } from '../../mocks/analysis';

export function AccountMenu({ onSelect }: { onSelect: (section: string) => void }) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false); };
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') { setOpen(false); trigger.current?.focus(); } };
    document.addEventListener('pointerdown', outside);
    document.addEventListener('keydown', escape);
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape); };
  }, [open]);
  return <div className="account" ref={root} onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) setOpen(false); }}>
    <button ref={trigger} className="accountTrigger" aria-expanded={open} aria-controls="account-panel" onClick={() => setOpen(!open)}><span className="accountText"><strong>{demoUser.name}</strong><small>{demoUser.description}</small></span><span className="avatar">AJ</span><ChevronDown size={18} className={open ? 'rotated' : ''} /></button>
    {open && <div id="account-panel" className="accountPanel"><div className="accountPanelHeading"><strong>{demoUser.name}</strong><small>{demoUser.description}</small></div><button onClick={() => { setOpen(false); onSelect('Mi perfil'); }}><UserRound size={19} />Mi perfil</button><button onClick={() => { setOpen(false); onSelect('Preferencias'); }}><Settings2 size={19} />Preferencias</button><div className="logout"><button disabled><LogOut size={19} />Cerrar sesión</button><small>Disponible al incorporar autenticación</small></div></div>}
  </div>;
}
