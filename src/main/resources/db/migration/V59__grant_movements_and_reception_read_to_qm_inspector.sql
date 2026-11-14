-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V59
-- Descripcion: Asignacion de permisos de lectura operativa de recepciones, salidas
--              y movimientos para el rol QM_INSPECTOR (Inspector de Calidad).
-- ==============================================================================

SET search_path TO wms, public;

INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'QM_INSPECTOR'
  AND p.name IN (
      'WAREHOUSE_MOVEMENTS_READ',
      'WAREHOUSE_MOVEMENTS_UPDATE',
      'RECEIVING_READ',
      'SHIPPING_READ',
      'SECURITY_GATE_READ',
      'AUDIT_READ'
  )
ON CONFLICT DO NOTHING;
