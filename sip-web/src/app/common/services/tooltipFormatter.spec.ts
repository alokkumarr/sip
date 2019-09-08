import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { getTooltipFormatter, getTooltipFormats } from './tooltipFormatter';


describe('Analyze Service', () => {

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: []
    });

  });


  it('should call formatter function', () => {
    const response = getTooltipFormatter({}, 'column');
    expect(response).toBeTruthy();
  });

  it('should fetch all tool tip formats', () => {
    const response = getTooltipFormats({}, 'column');
    expect(response).toBeTruthy();
  });
});
