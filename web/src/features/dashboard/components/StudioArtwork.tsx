/** Decorative paper study, intentionally separate from student work. */
export function StudioArtwork({ large = false }: { large?: boolean }): JSX.Element {
  return (
    <div
      className={large ? 'relative mx-auto h-[340px] w-[320px]' : 'relative h-[190px] w-[240px]'}
      aria-hidden
    >
      <div className="studio-paper absolute left-[13%] top-[10%] h-[76%] w-[66%] -rotate-[12deg] rounded-[3px] bg-[#e9dccc]" />
      <div className="studio-paper absolute left-[23%] top-[8%] h-[80%] w-[65%] rotate-[9deg] overflow-hidden rounded-[3px]">
        <svg viewBox="0 0 200 240" className="h-full w-full" preserveAspectRatio="xMidYMid slice">
          <rect width="200" height="240" fill="#fcfaf4" />
          <circle cx="134" cy="62" r="30" fill="#d39a68" />
          <path
            d="M-20 240V174C18 163 29 94 75 96C131 95 96 166 149 156C177 151 211 168 220 191V240Z"
            fill="#92aaa0"
          />
          <path d="M-10 230V204C33 191 45 156 88 179C136 205 156 175 210 193V240Z" fill="#c6d3b7" />
          <path d="M29 213C50 171 110 175 153 137" fill="none" stroke="#fcfaf4" strokeWidth="3" />
          <path
            d="M102 235C110 206 140 202 164 186"
            fill="none"
            stroke="#728a7d"
            strokeWidth="1.5"
          />
        </svg>
      </div>
      <div className="absolute left-[20%] top-[7%] h-5 w-16 -rotate-[13deg] bg-[#e4d3bd]/65" />
      <div className="absolute bottom-[9%] left-[7%] h-[6px] w-[72%] -rotate-[27deg] rounded-full bg-[#b56f4e] shadow-md">
        <span className="absolute -right-3 top-0 border-y-[3px] border-l-[13px] border-y-transparent border-l-[#dac6a8]" />
      </div>
    </div>
  );
}
