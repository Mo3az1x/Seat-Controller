/*
 * rte_types.h - RTE public types
 */

#ifndef RTE_TYPES_H
#define RTE_TYPES_H

#include <stdint.h>

typedef struct {
	uint16_t height;
	uint16_t slide;
	uint16_t incline;
} Rte_SeatControlReq_t;

typedef struct {
	uint16_t seatHeight;
	uint16_t seatSlide;
	uint16_t seatIncline;
} Rte_SensorCache_t;

#endif /* RTE_TYPES_H */
