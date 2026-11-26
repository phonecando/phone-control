import type { PhoneTool } from "../tool.ts"
import { appTools } from "./app-tools.ts"
import { pceTools } from "./pce-tools.ts"
import { screenTools } from "./screen-tools.ts"

// Phone control (full channel only). This folder is left out of the lite
// and play agent configs, so those builds carry no PCE or accessibility
// tool code at all.
export const controlTools: PhoneTool[] = [...pceTools, ...screenTools, ...appTools]
