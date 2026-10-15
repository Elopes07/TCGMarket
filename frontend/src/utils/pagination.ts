export function pageItems(current: number, total: number): (number | 'gap')[] {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1)
  const pages = [1, current - 1, current, current + 1, total]
    .filter((number) => number >= 1 && number <= total)
    .filter((number, index, list) => list.indexOf(number) === index)
    .sort((a, b) => a - b)
  return pages.flatMap((number, index) =>
    index > 0 && number - pages[index - 1] > 1
      ? (['gap', number] as const)
      : [number],
  )
}
