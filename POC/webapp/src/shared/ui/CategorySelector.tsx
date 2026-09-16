import { categories } from '../model/categories';
import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import s from './CategorySelector.module.css';

export function CategorySelector({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const selected = categories.findIndex(category => category.id === value);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(selected);
  const root = useRef<HTMLDivElement>(null);
  const search = useRef({ text: '', time: 0 });
  useEffect(() => {
    if (!open) return;
    const outside = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, [open]);
  useEffect(() => {
    if (open) root.current?.querySelector(`#category-option-${active}`)?.scrollIntoView({ block: 'nearest' });
  }, [open, active]);
  function keyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === 'Tab') { setOpen(false); return; }
    if (event.key === 'Escape') { event.preventDefault(); setOpen(false); return; }
    if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
      event.preventDefault();
      setOpen(true);
      setActive(event.key === 'Home' ? 0 : event.key === 'End' ? categories.length - 1 :
        !open ? selected : Math.max(0, Math.min(categories.length - 1, active + (event.key === 'ArrowDown' ? 1 : -1))));
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (open) { onChange(categories[active].id); setOpen(false); }
      else { setActive(selected); setOpen(true); }
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault();
      const text = (Date.now() - search.current.time < 700 ? search.current.text : '') + event.key;
      search.current = { text, time: Date.now() };
      const match = categories.findIndex(category => category.name.toLocaleLowerCase('es').startsWith(text.toLocaleLowerCase('es')));
      if (match >= 0) { setActive(match); setOpen(true); }
    }
  }
  return <div ref={root} className={s.root} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}>
    <label id="category-label" htmlFor="category">Categoría</label>
    <button type="button" id="category" role="combobox" aria-labelledby="category-label category-value" aria-haspopup="listbox" aria-expanded={open} aria-controls="category-options" aria-activedescendant={open ? `category-option-${active}` : undefined} aria-describedby="category-description category-availability" className={s.trigger} onKeyDown={keyDown} onClick={() => { setActive(selected); setOpen(!open); }}>
      <span id="category-value">{categories[selected].name}</span><ChevronDown size={18} />
    </button>
    {open && <ul id="category-options" role="listbox" aria-labelledby="category-label" className={s.options}>
      {categories.map((category, index) => <li id={`category-option-${index}`} key={category.id} role="option" aria-selected={category.id === value} className={index === active ? s.active : undefined} onPointerDown={event => event.preventDefault()} onClick={() => { onChange(category.id); setOpen(false); }}><span>{category.name}</span>{category.id === value && <Check size={18} aria-hidden="true" />}</li>)}
    </ul>}
  </div>;
}
