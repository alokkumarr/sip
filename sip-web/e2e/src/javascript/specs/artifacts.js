export const artifacts = [
  {
    artifactName: 'MCT_CONTENT_SUMMARY',
    columns: [
      {
        name: 'AVAILABLE_BYTES',
        type: 'long',
        columnName: 'AVAILABLE_BYTES',
        displayName: 'Available Bytes',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'AVAILABLE_ITEMS',
        type: 'long',
        columnName: 'AVAILABLE_ITEMS',
        displayName: 'Available Items',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'AVAILABLE_MB',
        type: 'float',
        columnName: 'AVAILABLE_MB',
        displayName: 'Available MB',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true,
        checked: true,
        area: 'data',
        dataType: 'number',
        format: {
          type: 'decimal',
          precision: 2
        },
        areaIndex: 0,
        aggregate: 'sum'
      },
      {
        name: 'FAILED_BYTES',
        type: 'long',
        columnName: 'FAILED_BYTES',
        displayName: 'Failed Bytes',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'FAILED_ITEMS',
        type: 'long',
        columnName: 'FAILED_ITEMS',
        displayName: 'Failed Items',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'FAILED_MB',
        type: 'float',
        columnName: 'FAILED_MB',
        displayName: 'Failed MB',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'SELECTED_BYTES',
        type: 'long',
        columnName: 'SELECTED_BYTES',
        displayName: 'Selected Bytes',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'SELECTED_ITEMS',
        type: 'long',
        columnName: 'SELECTED_ITEMS',
        displayName: 'Selected Items',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'SERVER_DIM_DATE',
        type: 'string',
        columnName: 'SERVER_DIM_DATE',
        displayName: 'Server Date',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true,
        checked: true,
        area: 'column',
        dataType: 'string',
        areaIndex: 0
      },
      {
        name: 'SESSION_ID',
        type: 'string',
        columnName: 'SESSION_ID',
        displayName: 'Session ID',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: true,
        filterEligible: true
      },
      {
        name: 'SESSION_STATUS',
        type: 'string',
        columnName: 'SESSION_STATUS',
        displayName: 'Session Status',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'SOURCE_MANUFACTURER',
        type: 'string',
        columnName: 'SOURCE_MANUFACTURER',
        displayName: 'Source Manufacturer',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'SOURCE_OS',
        type: 'string',
        columnName: 'SOURCE_OS',
        displayName: 'Source OS',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true,
        checked: true,
        area: 'row',
        dataType: 'string',
        areaIndex: 0
      },
      {
        name: 'SUMMARY_TYPE',
        type: 'string',
        columnName: 'SUMMARY_TYPE',
        displayName: 'Summary Type',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TARGET_APP_VERSION',
        type: 'string',
        columnName: 'TARGET_APP_VERSION',
        displayName: 'Target App. Version',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TARGET_MANUFACTURER',
        type: 'string',
        columnName: 'TARGET_MANUFACTURER',
        displayName: 'Target Manufacturer',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TARGET_OS',
        type: 'string',
        columnName: 'TARGET_OS',
        displayName: 'Target OS',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TRANSFER_BYTES',
        type: 'long',
        columnName: 'TRANSFER_BYTES',
        displayName: 'Transfer Bytes',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TRANSFER_ITEMS',
        type: 'long',
        columnName: 'TRANSFER_ITEMS',
        displayName: 'Transfer Items',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      },
      {
        name: 'TRANSFER_MB',
        type: 'float',
        columnName: 'TRANSFER_MB',
        displayName: 'Transfer MB',
        aliasName: '',
        table: 'MCT_CONTENT_SUMMARY',
        joinEligible: false,
        filterEligible: true
      }
    ]
  }
];
