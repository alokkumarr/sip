const methods = [
  {
    label: 'TABLES',
    category: 'table',
    children: [
      {
        label: 'Report',
        icon: {font: 'icon-report'},
        type: 'table:report'
      },
      {
        label: 'Pivot',
        icon: {font: 'icon-pivot'},
        type: 'table:pivot'
      }
    ]
  },
  {
    label: 'CHARTS',
    category: 'charts',
    children: [
      {
        label: 'Column Chart',
        icon: {font: 'icon-vert-bar-chart'},
        type: 'chart:column'
      },
      {
        label: 'Bar Chart',
        icon: {font: 'icon-hor-bar-chart'},
        type: 'chart:bar'
      },
      {
        label: 'Stacked Chart',
        icon: {font: 'icon-vert-bar-chart'},
        type: 'chart:stack'
      },
      {
        label: 'Line Chart',
        icon: {font: 'icon-line-chart'},
        type: 'chart:line'
      },
      {
        label: 'Area Chart',
        icon: {font: 'icon-area-chart'},
        type: 'chart:area'
      },
      {
        label: 'Combo Chart',
        icon: {font: 'icon-combo-chart'},
        type: 'chart:combo'
      },
      {
        label: 'Scatter Plot',
        icon: {font: 'icon-scatter-chart'},
        type: 'chart:scatter'
      },
      {
        label: 'Bubble Chart',
        icon: {font: 'icon-bubble-chart'},
        type: 'chart:bubble'
      },
      {
        label: 'Pie Chart',
        icon: {font: 'icon-pie-chart'},
        type: 'chart:pie'
      }
    ]
  }
];

export default methods;
