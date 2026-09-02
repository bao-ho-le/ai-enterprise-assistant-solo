// Shared column layout for the File Storage list. The header row lives outside
// the scroll container (see DocumentTable.js) while every row — folder or
// document — scrolls independently below it, so there's no single <table>
// anymore to compute column widths once and have header/body reuse them for
// free. This is that single source of truth instead: the exact same
// grid-template-columns string, used by the header row and every row
// component (DocumentRow, FolderRow), so their columns always land in the
// same place regardless of each row's own content.
//
// Fixed rem widths, not content-based auto-sizing — two independent grid
// containers can't share an auto-sized track the way one <table> could. File
// Name is the only flexible column (fills whatever space the fixed columns
// leave). Widths were sized off the app's real rendered columns plus a margin
// for the longest realistic content per column (e.g. Document Type needs to
// fit "Meeting Minutes", not just its own header label).
// File Name has a floor (16.5rem) below which it stops shrinking — past that
// point the table scrolls horizontally instead of squeezing the name into the
// checkbox column (see the horizontal-scroll wrapper in DocumentTable.js).
const BASE_COLUMNS = ["minmax(16.5rem,1fr)", "5rem", "9.5rem", "10rem", "6.75rem", "5rem"];
const MATCH_COLUMN = "5rem";
const TAIL_COLUMNS = ["7.5rem", "7.5rem"];

export function gridTemplateColumns(showSemanticColumn) {
  const columns = showSemanticColumn ? [...BASE_COLUMNS, MATCH_COLUMN] : BASE_COLUMNS;
  return [...columns, ...TAIL_COLUMNS].join(" ");
}
