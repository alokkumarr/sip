export function highchartsConfig(Highcharts) {
  'ngInject';

  Highcharts.setOptions({
    colors: ['#490094', '#9A72C4', '#C8B2DF', '#006ADE', '#6AB4FF',
      '#B5DAFF', '#014752', '#009293', '#73C3C4', '#4CEA7C', '#9DF4B7', '#C9F9D8',
      '#DD5400', '#EDA173', '#F5CDB4', '#940000', '#C47373', '#DFB2B2'],
    plotOptions: {
      series: {
        barBgColor: '#f3f5f8',
        showCheckbox: true,
        marker: {
          fillColor: '#FFFFFF',
          lineWidth: 2,
          lineColor: null // inherit from series
        }
      },
      bar: {
        showCheckbox: true,
        dataLabels: {
          enabled: true,
          color: '#A4A9AD',
          style: {
            textShadow: 'none'
          }
        }
      }
    },
    legend: {
      align: 'left',
      verticalAlign: 'top',
      layout: 'vertical',
      y: 100
    },
    tooltip: {
      backgroundColor: '#293D5A',
      borderWidth: 0,
      shadow: false,
      headerFormat: '<span style="font-size: 12px; opacity: 0.8;">{point.key}</span><br/>',
      pointFormat: '<span style="color:{point.color}; stroke: white; stroke-width: 2; ' +
      'font-size: 25px;">\u25CF</span> {series.name}: <b>{point.y}</b><br/>',
      style: {
        color: '#FFFFFF',
        fontSize: '16px'
      }
    },
    title: false,
    credits: false
  });
}
